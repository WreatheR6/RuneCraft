package com.runecraft.content.minigames.games;

import com.runecraft.action.ActionManager;
import com.runecraft.combat.CombatHandler;
import com.runecraft.content.BlockHooks;
import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.GuardiansRift;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameRules;
import com.runecraft.osrs.content.minigames.SkillingBosses;
import com.runecraft.osrs.content.minigames.WintertodtRewards;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;

/**
 * Real-player self-test walks of the skilling bosses (round 38; {@code com.runecraft.selftest} "minigame:wintertodt_round",
 * "minigame:tempoross_game", "minigame:gotr_game", real-player mode, since minigame sessions only see players in the
 * server's player list). Each plays a whole game through the blocks a player clicks ({@link BlockHooks}) and the items a
 * player uses, ticking the session and the player's actions by hand, and throws an {@link AssertionError} naming any broken
 * step:
 * <ul>
 *   <li>Wintertodt: the supply crate's tools, bruma herbs mixed into rejuvenation potions, the braziers lit, roots chopped
 *       (not interrupted by the cold), kindling fletched with a knife (interrupted by the cold), the braziers fed, a shattered
 *       brazier fixed and relit, a fallen pyromancer helped, the Wintertodt subdued with 500+ points, the reward cart
 *       searched (one, then a big search).</li>
 *   <li>Tempoross: the supply crate and the water pump, harpoonfish caught and cooked, a wave survived tethered to a totem,
 *       a fire doused, a broken totem repaired, both cannons loaded until he submerges (the storm bar resets), the spirit pool harpooned through a
 *       whole essence phase (energy recharging) and a second one, permits, the reward pool fished with a net.</li>
 *   <li>Guardians of the Rift: guardian remains and the large remains mined, essence at the workbench, cells charged at the
 *       altars, a barrier raised, a rift guardian assembled, stones given to the Great Guardian, the huge remains through
 *       the portal, the rift's creatures (they can't be hurt, no combat XP), the rift sealed, points and the Rewards
 *       Guardian.</li>
 *   <li>Volcanic Mine ("minigame:volcanic_mine_exit"): only the rope exit pays; the leave command forfeits the fragments.</li>
 * </ul>
 */
public final class SkillingBossWalk {
    private final ServerPlayer player;
    private final ServerLevel world;
    private final List<String> notes = new ArrayList<>();
    private String step = "start";
    private MinigameSession session;
    private int ticks;
    /** Run before every session tick: what a player keeps doing in the background (eating, tethering, dodging fires). */
    private Runnable perTick = () -> { };
    /** Tempoross: totem poles the player repaired before a wave because earlier waves had broken both. */
    private int polesRepaired;

    private SkillingBossWalk(ServerPlayer player) {
        this.player = player;
        this.world = (ServerLevel) player.level();
    }

    private interface Body {
        void run(SkillingBossWalk w);
    }

    private static String run(ServerPlayer p, Body body) {
        SkillingBossWalk w = new SkillingBossWalk(p);
        try {
            body.run(w);
            return w.ticks + " game ticks: " + String.join("; ", w.notes);
        } catch (AssertionError e) {
            throw new AssertionError("[" + w.step + "] " + e.getMessage() + " (after " + w.ticks + " ticks; done: "
                    + String.join("; ", w.notes) + ")");
        } finally {
            ActionManager.cancel(p);
            if (w.session != null && !w.session.isEnded()) {
                w.session.end(false);
            }
        }
    }

    public static String wintertodt(ServerPlayer p) {
        return run(p, SkillingBossWalk::wintertodtRound);
    }

    public static String tempoross(ServerPlayer p) {
        return run(p, SkillingBossWalk::temporossGame);
    }

    public static String guardians(ServerPlayer p) {
        return run(p, SkillingBossWalk::guardiansGame);
    }

    public static String volcanicMineExit(ServerPlayer p) {
        return run(p, SkillingBossWalk::volcanicMineExit);
    }

    // ------------------------------------------------------------------ helpers

    private static void expect(boolean ok, String message) {
        if (!ok) {
            throw new AssertionError(message);
        }
    }

    private String blockId(BlockPos pos) {
        return BuiltInRegistries.BLOCK.getKey(world.getBlockState(pos).getBlock()).getPath();
    }

    /** Clicks a block as a player would, after walking next to it; checks the block is there. */
    private void use(BlockPos pos, String id) {
        expect(id.equals(blockId(pos)), "no " + id + " at " + pos + " (found " + blockId(pos) + ")");
        moveNextTo(pos);
        BlockHooks.use(player, id, pos, world.getBlockState(pos));
    }

    private void moveNextTo(BlockPos pos) {
        moveTo(pos.east());
    }

    private void moveTo(BlockPos stand) {
        player.teleportTo(world, stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, player.getYRot(), 0);
        player.fallDistance = 0;
    }

    /** One game tick: the player's action, then the session; the player is kept alive. */
    private void tick() {
        for (int m = 0; m < ActionManager.MC_TICKS_PER_GAME_TICK; m++) {
            ActionManager.advanceForSelfTest();
        }
        ActionManager.tickNow(player);
        perTick.run();
        if (session != null && !session.isEnded()) {
            MinigameManager.selfTestTick(session);
        }
        if (player.getHealth() < player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        ticks++;
    }

    /** Ticks until {@code done} holds, at most {@code max} ticks; false when it never did. */
    private boolean until(int max, BooleanSupplier done) {
        for (int i = 0; i < max; i++) {
            if (done.getAsBoolean()) {
                return true;
            }
            tick();
        }
        return done.getAsBoolean();
    }

    private double xp(Skill s) {
        return Players.data(player).skills().xpTenths(s) / 10.0;
    }

    private MinigameSession start(Minigame game) {
        for (String id : new String[]{"knife", "hammer", "tinderbox", "bronze_axe", "rope", "harpoon", "minecraft:bucket",
                "minecraft:water_bucket", "reward_permit"}) {
            Inv.remove(player, id, Inv.count(player, id));
        }
        session = MinigameManager.selfTestStart(game, game.entranceBlock(), player);
        expect(session != null && MinigameManager.session(player) == session, game.displayName() + " didn't start a session");
        return session;
    }

    // ------------------------------------------------------------------ Wintertodt

    private void wintertodtRound() {
        step = "lobby";
        WintertodtSession w = (WintertodtSession) start(Minigame.WINTERTODT);
        use(w.selfTestCrate(), "wintertodt_crate");
        for (String tool : new String[]{"knife", "hammer", "tinderbox", "bronze_axe"}) {
            expect(Inv.has(player, tool, 1), "the supply crate didn't hand out a " + tool);
        }
        use(w.selfTestHerbs(), "sprouting_roots");
        use(w.selfTestHerbs(), "sprouting_roots");
        expect(Inv.count(player, "bruma_herb") == 2, "the sprouting roots gave " + Inv.count(player, "bruma_herb") + " herbs for 2 picks");
        use(w.selfTestCrate(), "wintertodt_crate");
        expect(Inv.count(player, "rejuvenation_potion") == 2 && !Inv.has(player, "bruma_herb", 1),
                "the crate didn't mix 2 herbs into rejuvenation potions");
        notes.add("tools and 2 rejuvenation potions from the crate");

        perTick = () -> keepWarm(w);
        step = "braziers";
        for (int i = 0; i < 4; i++) {
            use(w.selfTestBrazier(i), "wintertodt_brazier");
        }
        expect(w.selfTestView(player).lit() == 4 && w.selfTestView(player).points() == 100,
                "lighting the 4 braziers: " + w.selfTestView(player).lit() + " lit, " + w.selfTestView(player).points() + " points");
        notes.add("4 braziers lit (100 points)");

        step = "chop";
        double wc = xp(Skill.WOODCUTTING);
        use(w.selfTestRoots(2), "bruma_roots");
        expect(ActionManager.isBusy(player), "clicking the bruma roots didn't start chopping");
        w.selfTestChill(player);
        expect(ActionManager.isBusy(player), "the cold interrupted chopping (it never does)");
        keepWarm(w);
        expect(until(80, () -> Inv.count(player, "bruma_root") >= 12), "80 ticks chopping gave " + Inv.count(player, "bruma_root")
                + " roots");
        ActionManager.cancel(player);
        expect(xp(Skill.WOODCUTTING) > wc, "chopping gave no Woodcutting XP");

        step = "fletch";
        moveTo(w.selfTestBrazier(2).east().east());
        expect(WintertodtSession.useKnife(player, "knife"), "using a knife on bruma roots did nothing");
        expect(ActionManager.isBusy(player), "the knife didn't start fletching");
        tick();
        int kindling = Inv.count(player, "bruma_kindling");
        expect(kindling >= 1, "a tick of fletching made no kindling");
        w.selfTestChill(player);
        expect(!ActionManager.isBusy(player), "the cold didn't interrupt fletching");
        keepWarm(w);
        fletchAll(w);
        notes.add("12 roots chopped, the cold interrupted fletching but not chopping, " + Inv.count(player, "bruma_kindling")
                + " kindling");

        step = "feed";
        feedAll(w, 2);
        int points = w.selfTestView(player).points();
        expect(points >= 100 + 12 * 25 - 1, "feeding 12 kindling: " + points + " points");
        notes.add("fed the brazier (" + points + " points)");

        step = "brazier attacks";
        w.selfTestShatter(1);
        expect(w.selfTestBrazierState(1).startsWith("BROKEN"), "a shattered brazier is " + w.selfTestBrazierState(1));
        double con = xp(Skill.CONSTRUCTION);
        for (int i = 0; i < 3 && w.selfTestBrazierState(1).startsWith("BROKEN"); i++) {
            use(w.selfTestBrazier(1), "wintertodt_brazier"); // a hurt pyromancer is helped first, then the brazier fixed
        }
        expect(!w.selfTestBrazierState(1).startsWith("BROKEN") && xp(Skill.CONSTRUCTION) > con, "fixing the brazier with a hammer: "
                + w.selfTestBrazierState(1));
        for (int i = 0; i < 3 && !w.selfTestBrazierState(1).startsWith("LIT"); i++) {
            use(w.selfTestBrazier(1), "wintertodt_brazier");
        }
        expect(w.selfTestBrazierState(1).startsWith("LIT"), "relighting the fixed brazier: " + w.selfTestBrazierState(1));
        if (!Inv.has(player, "rejuvenation_potion", 1)) {
            Inv.give(player, "rejuvenation_potion", 1);
        }
        w.selfTestHitPyromancer(0);
        w.selfTestHitPyromancer(0);
        expect(w.selfTestBrazierState(0).endsWith(" 0") && !w.selfTestBrazierState(0).startsWith("LIT"),
                "two hits on a pyromancer: " + w.selfTestBrazierState(0));
        int before = w.selfTestView(player).points();
        use(w.selfTestBrazier(0), "wintertodt_brazier");
        expect(w.selfTestBrazierState(0).endsWith(" " + MinigameRules.Wintertodt.PYROMANCER_HP)
                        && w.selfTestView(player).points() == before + MinigameRules.Wintertodt.POINTS_HEAL_PYROMANCER,
                "helping the fallen pyromancer: " + w.selfTestBrazierState(0));
        use(w.selfTestBrazier(0), "wintertodt_brazier");
        notes.add("fixed and relit a shattered brazier, helped a fallen pyromancer (+75)");

        step = "more kindling";
        for (int round = 0; round < 6 && w.selfTestView(player).points() < 600; round++) {
            use(w.selfTestRoots(2), "bruma_roots");
            until(60, () -> Inv.count(player, "bruma_root") >= 10);
            ActionManager.cancel(player);
            fletchAll(w);
            feedAll(w, 2);
        }
        int pts = w.selfTestView(player).points();
        expect(pts >= WintertodtRewards.MIN_POINTS, "only " + pts + " points after six chop-fletch-feed rounds");

        step = "subdue";
        int kc = MinigameData.of(player).stat(player.getUUID(), "kc:wintertodt");
        int owedBefore = MinigameData.of(player).stat(player.getUUID(), WintertodtSession.CART_STAT);
        double fm = xp(Skill.FIREMAKING);
        w.selfTestEnergy(2);
        expect(until(120, () -> {
            for (int i = 0; i < 4 && !w.isEnded(); i++) {
                if (w.selfTestBrazierState(i).startsWith("BROKEN") || w.selfTestBrazierState(i).startsWith("UNLIT")) {
                    use(w.selfTestBrazier(i), "wintertodt_brazier");
                }
            }
            return w.isEnded();
        }), "the Wintertodt at 2% energy wasn't subdued in 120 ticks (energy " + w.selfTestView(player).energy() + "%)");
        int owed = MinigameData.of(player).stat(player.getUUID(), WintertodtSession.CART_STAT) - owedBefore;
        expect(owed >= WintertodtRewards.rolls(pts), pts + " points owed " + owed + " rewards at the cart");
        expect(MinigameData.of(player).stat(player.getUUID(), "kc:wintertodt") == kc + 1, "the subdue count didn't go up");
        expect(xp(Skill.FIREMAKING) - fm >= MinigameRules.Wintertodt.completionXp(99) - 1, "no bonus Firemaking XP for 500+ points");
        notes.add("subdued with " + pts + " points: " + owed + " rewards owed, bonus Firemaking XP");

        step = "reward cart";
        perTick = () -> { };
        player.getInventory().clearContent();
        int searched = MinigameData.of(player).stat(player.getUUID(), "wintertodt_cart_searched");
        int taken = WintertodtSession.takeFromCart(player, 1);
        int left = MinigameData.of(player).stat(player.getUUID(), WintertodtSession.CART_STAT);
        expect(taken == 1 && left == owedBefore + owed - 1
                && MinigameData.of(player).stat(player.getUUID(), "wintertodt_cart_searched") == searched + 1,
                "searching the cart once: taken " + taken + ", " + left + " left");
        int big = WintertodtSession.takeFromCart(player, WintertodtRewards.BIG_SEARCH);
        expect(big == Math.min(WintertodtRewards.BIG_SEARCH, left), "a big search took " + big);
        WintertodtSession.rewardCart(player);
        expect(until(3 * 40, () -> MinigameData.of(player).stat(player.getUUID(), WintertodtSession.CART_STAT) == 0
                || !ActionManager.isBusy(player)), "the cart search action never finished");
        notes.add("searched the reward cart (1, a big search of " + big + ", then the rest: "
                + MinigameData.of(player).stat(player.getUUID(), WintertodtSession.CART_STAT) + " left)");
        MinigameData.of(player).setStat(player.getUUID(), WintertodtSession.CART_STAT, owedBefore);
    }

    /** Fletches every carried root (outside the safe area), starting again whenever the cold interrupts it. */
    private void fletchAll(WintertodtSession w) {
        moveTo(w.selfTestBrazier(2).east().east());
        expect(until(60, () -> {
            if (Inv.has(player, "bruma_root", 1) && !ActionManager.isBusy(player)) {
                WintertodtSession.useKnife(player, "knife");
            }
            return !Inv.has(player, "bruma_root", 1);
        }), "fletching left " + Inv.count(player, "bruma_root") + " roots");
    }

    /** Feeds every root and kindling into a brazier, relighting or fixing it if the Wintertodt got to it. */
    private void feedAll(WintertodtSession w, int i) {
        if (!Inv.has(player, "rejuvenation_potion", 1)) {
            Inv.give(player, "rejuvenation_potion", 2); // a fallen pyromancer may need help before the brazier can be relit
        }
        for (int tries = 0; tries < 12 && (Inv.has(player, "bruma_kindling", 1) || Inv.has(player, "bruma_root", 1)); tries++) {
            if (!w.selfTestBrazierState(i).startsWith("LIT")) {
                use(w.selfTestBrazier(i), "wintertodt_brazier"); // fix, heal or light
                continue;
            }
            use(w.selfTestBrazier(i), "wintertodt_brazier");
            until(30, () -> !ActionManager.isBusy(player));
        }
        expect(!Inv.has(player, "bruma_kindling", 1) && !Inv.has(player, "bruma_root", 1), "couldn't feed everything into brazier " + i
                + " (" + w.selfTestBrazierState(i) + ")");
    }

    private void keepWarm(WintertodtSession w) {
        if (w.selfTestView(player).warmth() < 60) {
            w.selfTestWarm(player, "shark");
        }
    }

    // ------------------------------------------------------------------ Tempoross

    private void temporossGame() {
        step = "boat";
        TemporossSession t = (TemporossSession) start(Minigame.TEMPOROSS);
        expect(Inv.has(player, "harpoon", 1), "First Mate Deri didn't hand out a harpoon");
        use(t.selfTestSupplies(), "tempoross_supplies");
        for (String tool : new String[]{"rope", "hammer", MinigameRules.Tempoross.BUCKET}) {
            expect(Inv.has(player, tool, 1), "the supply crate didn't hand out " + tool);
        }
        use(t.selfTestPump(), "tempoross_water_pump");
        expect(Inv.has(player, MinigameRules.Tempoross.WATER_BUCKET, 1), "the water pump didn't fill the bucket");
        notes.add("rope, hammer and a bucket filled at the pump");

        perTick = () -> {
            surviveWave(t);
            stepOutOfFire(t);
        };
        step = "fish and cook";
        catchAndCook(t, 18);
        notes.add("18 harpoonfish caught and cooked");

        step = "wave";
        int pole = standingTotem(t);
        t.selfTestWaveIn(MinigameRules.Tempoross.WAVE_WARNING);
        use(t.selfTestTotem(pole), "tempoross_totem");
        expect(t.selfTestView(player).tethered(), "the rope didn't tether the player to the totem pole");
        expect(until(MinigameRules.Tempoross.WAVE_WARNING + 2, () -> t.selfTestView(player).nextWave() > MinigameRules.Tempoross.WAVE_WARNING),
                "the wave never came");
        expect(!t.caFlags(player).contains(com.runecraft.osrs.content.systems.CombatAchievements.FLAG_WAVE_HIT),
                "the wave hit a player tethered to a totem pole");
        notes.add("tethered through a colossal wave");

        step = "fire";
        ensureTools(t);
        t.selfTestLightning();
        BlockPos fire = t.selfTestFire();
        expect(fire != null, "lightning lit no fire");
        int pts = t.selfTestView(player).points();
        use(fire, "tempoross_fire");
        expect(t.selfTestView(player).points() == pts + MinigameRules.Tempoross.POINTS_DOUSE
                && Inv.has(player, MinigameRules.Tempoross.BUCKET, 1), "dousing a fire: points " + pts + " -> " + t.selfTestView(player).points());
        use(t.selfTestPump(), "tempoross_water_pump");
        notes.add("a fire doused (+40)");

        step = "totem";
        ensureTools(t);
        int other = standingTotem(t) == 0 ? 1 : 0;
        if (!t.selfTestTotemStanding(other)) {
            other = standingTotem(t);
        }
        BlockPos broken = t.selfTestBreakTotem(other);
        double con = xp(Skill.CONSTRUCTION);
        use(broken, "tempoross_broken_totem");
        expect("tempoross_totem".equals(blockId(broken)) && xp(Skill.CONSTRUCTION) > con, "repairing the broken totem pole");
        notes.add("a broken totem pole repaired");

        step = "cannons";
        int cooked = Inv.count(player, "harpoonfish");
        expect(cooked >= 14, "only " + cooked + " cooked harpoonfish left to load");
        if (cooked < 18) {
            Inv.give(player, "harpoonfish", 18 - cooked); // approx: what a fire or wave took, caught again
        }
        int energy = t.selfTestView(player).energy();
        int storm = t.selfTestView(player).storm();
        expect(storm > 0 && storm < 100, "the storm intensity is " + storm + "% after " + ticks + " ticks of the first energy phase");
        Inv.remove(player, "harpoonfish", 9);
        load(t, 0);
        expect(t.selfTestView(player).energy() < energy, "loading the first cannon didn't drain his energy");
        Inv.give(player, "harpoonfish", 9);
        load(t, 1);
        expect(t.selfTestView(player).submerged(), "18 cooked harpoonfish didn't bring his energy to 0 (energy "
                + t.selfTestView(player).energy() + "%)");
        expect(t.selfTestView(player).storm() == 0, "the storm intensity stayed at " + t.selfTestView(player).storm()
                + "% when he submerged (it resets to 0%)");
        notes.add("both cannons loaded at " + storm + "% storm: he submerges and the storm resets");

        step = "spirit pool";
        int essence = t.selfTestView(player).essence();
        use(t.selfTestPool(), "tempoross_spirit_pool");
        expect(ActionManager.isBusy(player), "harpooning the spirit pool didn't start");
        expect(until(TemporossSessionTicks.PHASE + 10, () -> !t.selfTestView(player).submerged() || t.isEnded()),
                "the essence phase never ended");
        expect(!t.isEnded(), "one essence phase subdued a solo Tempoross (essence should last about two)");
        View v = view(t);
        expect(v.essence < essence && v.recoveries == 1 && v.energy == 100, "after the essence phase: essence " + essence + " -> "
                + v.essence + "%, recoveries " + v.recoveries + ", energy " + v.energy + "%");
        notes.add("a whole essence phase took his essence " + essence + " -> " + v.essence + "%, then he recovered");

        step = "second phase";
        ActionManager.cancel(player);
        catchAndCook(t, 18);
        if (Inv.count(player, "harpoonfish") < 18) {
            Inv.give(player, "harpoonfish", 18 - Inv.count(player, "harpoonfish"));
        }
        storm = t.selfTestView(player).storm();
        expect(storm > 0 && storm < 100, "the storm intensity is " + storm + "% in the second energy phase");
        load(t, 0);
        expect(t.selfTestView(player).submerged(), "18 more cooked harpoonfish didn't make him submerge again");
        expect(t.selfTestView(player).storm() == 0, "the storm intensity stayed at " + t.selfTestView(player).storm()
                + "% when he submerged again");
        notes.add("second phase loaded at " + storm + "% storm (the bar started again from 0%)"
                + (polesRepaired > 0 ? "; " + polesRepaired + " wave(s) had broken both totem poles: repaired one to tether" : ""));
        use(t.selfTestPool(), "tempoross_spirit_pool");
        int permits = Inv.count(player, "reward_permit");
        expect(until(TemporossSessionTicks.PHASE + 10, t::isEnded), "the second essence phase didn't subdue him (essence "
                + t.selfTestView(player).essence() + "%)");
        int got = Inv.count(player, "reward_permit") - permits;
        expect(got >= 1, "subduing Tempoross with " + t.selfTestView(player).points() + " points gave no permit");
        notes.add("subdued: " + got + " reward permits");

        step = "reward pool";
        perTick = () -> { };
        player.getInventory().clearContent();
        Inv.give(player, "reward_permit", 3);
        TemporossSession.rewardPool(player);
        expect(Inv.has(player, "small_fishing_net", 1) && Inv.count(player, "reward_permit") == 3,
                "the Spirit Angler didn't hand over a net first");
        double fishing = xp(Skill.FISHING);
        TemporossSession.rewardPool(player);
        expect(until(3 * 4, () -> Inv.count(player, "reward_permit") == 0), "fishing the pool left "
                + Inv.count(player, "reward_permit") + " permits");
        expect(Math.abs(xp(Skill.FISHING) - fishing - 3 * com.runecraft.osrs.content.minigames.TemporossRewards.POOL_XP) < 1,
                "the pool gave " + (xp(Skill.FISHING) - fishing) + " Fishing XP for 3 rewards");
        notes.add("fished 3 rewards out of the pool (10 Fishing XP each)");
    }

    private record View(int energy, int essence, int recoveries) {
    }

    private View view(TemporossSession t) {
        TemporossSession.View v = t.selfTestView(player);
        return new View(v.energy(), v.essence(), v.recoveries());
    }

    /** Tempoross timing the walk waits on. */
    private static final class TemporossSessionTicks {
        static final int PHASE = com.runecraft.osrs.content.minigames.TemporossStorm.ESSENCE_PHASE_TICKS;
    }

    /** Catches {@code n} raw harpoonfish at a spot and cooks them at the shrine, tethering through any wave on the way. */
    private void catchAndCook(TemporossSession t, int n) {
        double fishing = xp(Skill.FISHING);
        use(t.selfTestSpot(), "tempoross_fishing_spot");
        expect(until(n * 8, () -> {
            if (!ActionManager.isBusy(player) && Inv.count(player, "raw_harpoonfish") < n) {
                use(t.selfTestSpot(), "tempoross_fishing_spot");
            }
            return Inv.count(player, "raw_harpoonfish") >= n;
        }), "fishing gave " + Inv.count(player, "raw_harpoonfish") + " harpoonfish of " + n);
        ActionManager.cancel(player);
        expect(xp(Skill.FISHING) > fishing, "catching harpoonfish gave no Fishing XP");
        Inv.remove(player, "raw_harpoonfish", Inv.count(player, "raw_harpoonfish") - n);
        use(t.selfTestShrine(), "tempoross_shrine");
        expect(until(n * 4 + 10, () -> {
            if (!ActionManager.isBusy(player) && Inv.has(player, "raw_harpoonfish", 1)) {
                use(t.selfTestShrine(), "tempoross_shrine");
            }
            return !Inv.has(player, "raw_harpoonfish", 1);
        }), "cooking left " + Inv.count(player, "raw_harpoonfish") + " raw harpoonfish");
        // Only a wave (untethered) or a fire the player stood in takes fish (OSRS Wiki): say which, if any, did.
        expect(Inv.count(player, "harpoonfish") >= n, "cooking made " + Inv.count(player, "harpoonfish") + " harpoonfish of " + n
                + (t.caFlags(player).contains(com.runecraft.osrs.content.systems.CombatAchievements.FLAG_WAVE_HIT)
                        ? " (a wave hit the player this game)" : "")
                + (t.caFlags(player).contains(com.runecraft.osrs.content.systems.CombatAchievements.FLAG_FIRE_HIT)
                        ? " (a fire burnt the player's supplies this game)" : ""));
        Inv.remove(player, "harpoonfish", Inv.count(player, "harpoonfish") - n);
    }

    /** Loads a cannon, waiting out a torrent that knocked it out. */
    private void load(TemporossSession t, int cannon) {
        int fish = Inv.count(player, "harpoonfish") + Inv.count(player, "raw_harpoonfish");
        for (int i = 0; i < 20; i++) {
            use(t.selfTestCannon(cannon), "tempoross_cannon");
            if (Inv.count(player, "harpoonfish") + Inv.count(player, "raw_harpoonfish") < fish) {
                return;
            }
            tick(); // the torrent knocked the cannon out for a while
        }
        throw new AssertionError("cannon " + cannon + " never took the harpoonfish");
    }

    /** A standing totem pole (repairing one when a wave broke both). */
    private int standingTotem(TemporossSession t) {
        for (int i = 0; i < 2; i++) {
            if (t.selfTestTotemStanding(i)) {
                return i;
            }
        }
        ensureTools(t);
        use(t.selfTestTotem(0), "tempoross_broken_totem");
        expect(t.selfTestTotemStanding(0), "couldn't repair a broken totem pole");
        return 0;
    }

    /** Takes a rope, a hammer and a bucket again (a wave or fire may have taken them) and fills a bucket. */
    private void ensureTools(TemporossSession t) {
        if (!Inv.has(player, "rope", 1) || !Inv.has(player, "hammer", 1)
                || !(Inv.has(player, MinigameRules.Tempoross.BUCKET, 1) || Inv.has(player, MinigameRules.Tempoross.WATER_BUCKET, 1))) {
            BlockPos at = t.selfTestSupplies();
            BlockHooks.use(player, "tempoross_supplies", at, world.getBlockState(at));
        }
        if (!Inv.has(player, MinigameRules.Tempoross.WATER_BUCKET, 1)) {
            BlockPos at = t.selfTestPump();
            BlockHooks.use(player, "tempoross_water_pump", at, world.getBlockState(at));
        }
    }

    /** Steps two blocks aside when a fire burns where the player stands (fires burn harpoonfish and tools). */
    private void stepOutOfFire(TemporossSession t) {
        for (BlockPos f : t.selfTestFires()) {
            if (Math.abs(player.getX() - (f.getX() + 0.5)) < 1.5 && Math.abs(player.getZ() - (f.getZ() + 0.5)) < 1.5) {
                player.teleportTo(world, player.getX() + 3, player.getY(), player.getZ(), player.getYRot(), 0);
                return;
            }
        }
    }

    /**
     * Tethers to a totem pole when a colossal wave is about to land (from wherever the player stands). Each wave breaks each
     * pole with a 15-25% chance (OSRS Wiki "Tempoross") and a broken pole can't be tethered to until it is repaired with a
     * hammer ("Tempoross/Strategies"), so when earlier waves broke both, the player repairs one first, as a player would.
     */
    private void surviveWave(TemporossSession t) {
        TemporossSession.View v = t.selfTestView(player);
        if (v.nextWave() <= MinigameRules.Tempoross.WAVE_WARNING && v.nextWave() > 0 && !v.tethered() && !v.submerged()) {
            boolean noPole = !t.selfTestTotemStanding(0) && !t.selfTestTotemStanding(1);
            if (!Inv.has(player, "rope", 1) || (noPole && !Inv.has(player, "hammer", 1))) {
                BlockPos at = t.selfTestSupplies();
                BlockHooks.use(player, "tempoross_supplies", at, world.getBlockState(at));
            }
            if (noPole) {
                BlockPos broken = t.selfTestTotem(0);
                BlockHooks.use(player, "tempoross_broken_totem", broken, world.getBlockState(broken));
                polesRepaired++;
            }
            for (int i = 0; i < 2; i++) {
                BlockPos totem = t.selfTestTotem(i);
                if (t.selfTestTotemStanding(i)) {
                    BlockHooks.use(player, "tempoross_totem", totem, world.getBlockState(totem));
                    return;
                }
            }
        }
    }

    // ------------------------------------------------------------------ Volcanic Mine

    /**
     * Two Volcanic Mine games: leaving the first with the leave command (a teleport out: the ore fragments disintegrate, no
     * points or XP), then the second through the rope exit (Petrified Pete pays the points and 3.8 Mining XP a point).
     */
    private void volcanicMineExit() {
        // The balance Petrified Pete's Ore Shop spends: stored under the currency's key (MinigameSession.addPoints).
        String vm = com.runecraft.osrs.content.minigames.MinigameCurrencies.key(
                com.runecraft.osrs.content.minigames.MinigameCurrencies.VOLCANIC_MINE);
        int earned = 200;
        for (boolean rope : new boolean[]{false, true}) {
            step = rope ? "rope exit" : "leave command";
            Inv.give(player, "numulite", com.runecraft.osrs.content.minigames.VolcanicMine.ENTRY_NUMULITE);
            VolcanicMineSession s = (VolcanicMineSession) start(Minigame.VOLCANIC_MINE);
            expect(!s.isEnded(), "Petrified Pete didn't let the player in with 30 numulite");
            s.selfTestAddPoints(player, earned);
            int held = MinigameData.of(player).points(player.getUUID(), vm);
            double mining = xp(Skill.MINING);
            if (rope) {
                use(s.selfTestExit(), "minigame_exit");
            } else {
                MinigameManager.leave(player);
            }
            expect(MinigameManager.session(player) == null, "the player is still in the mine after leaving");
            int got = MinigameData.of(player).points(player.getUUID(), vm) - held;
            double gotXp = xp(Skill.MINING) - mining;
            if (rope) {
                int paid = Math.min(earned, com.runecraft.osrs.content.minigames.VolcanicMine.MAX_POINTS - held);
                expect(got == paid && Math.abs(gotXp - com.runecraft.osrs.content.minigames.VolcanicMine.endXp(paid)) < 1,
                        "the rope exit paid " + got + " points and " + gotXp + " Mining XP for " + earned + " points");
                notes.add("the rope exit paid " + got + " points and " + Math.round(gotXp) + " Mining XP");
            } else {
                expect(got == 0 && gotXp < 0.05, "leaving with the command paid " + got + " points and " + gotXp + " Mining XP");
                notes.add("the leave command forfeited the fragments");
            }
        }
    }

    // ------------------------------------------------------------------ Guardians of the Rift

    private void guardiansGame() {
        step = "mining";
        GuardiansSession g = (GuardiansSession) start(Minigame.GUARDIANS_OF_THE_RIFT);
        for (String id : new String[]{"guardian_fragments", "guardian_essence", "chisel"}) {
            Inv.remove(player, id, Inv.count(player, id));
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, Inv.stack("rune_pickaxe", 1));
        Inv.give(player, "rune_pickaxe", 1);
        double mining = xp(Skill.MINING);
        use(g.selfTestRemains(), "guardian_remains");
        expect(until(120, () -> Inv.count(player, "guardian_fragments") >= 60), "120 ticks at the guardian remains gave "
                + Inv.count(player, "guardian_fragments") + " fragments");
        ActionManager.cancel(player);
        expect(xp(Skill.MINING) > mining, "mining the remains gave no Mining XP");
        use(g.selfTestLargeRemains(), SkillingBosses.GOTR_LARGE_REMAINS);
        int frags = Inv.count(player, "guardian_fragments");
        expect(until(40, () -> Inv.count(player, "guardian_fragments") >= frags + 10), "the large guardian remains gave "
                + (Inv.count(player, "guardian_fragments") - frags) + " fragments in 40 ticks");
        ActionManager.cancel(player);
        notes.add(Inv.count(player, "guardian_fragments") + " fragments from the remains and the large remains");

        step = "workbench";
        double crafting = xp(Skill.CRAFTING);
        int fragments = Inv.count(player, "guardian_fragments");
        use(g.selfTestWorkbench(), "gotr_workbench");
        expect(until(fragments, () -> !Inv.has(player, "guardian_fragments", 1)), "the workbench left "
                + Inv.count(player, "guardian_fragments") + " fragments");
        expect(Inv.count(player, "guardian_essence") == fragments, "the workbench made " + Inv.count(player, "guardian_essence")
                + " essence from " + fragments + " fragments");
        expect(Math.abs(xp(Skill.CRAFTING) - crafting - GuardiansRift.workbenchXp(99) * fragments) < 1,
                "the workbench gave " + (xp(Skill.CRAFTING) - crafting) + " Crafting XP for " + fragments + " essence");
        notes.add(fragments + " guardian essence at the workbench (5 Crafting XP each)");

        step = "cells and altars";
        use(g.selfTestCellTable(), "gotr_cell_table");
        expect(Inv.count(player, GuardiansRift.UNCHARGED_CELL) == GuardiansRift.MAX_UNCHARGED_CELLS, "the cell table gave "
                + Inv.count(player, GuardiansRift.UNCHARGED_CELL) + " cells");
        int half = Inv.count(player, "guardian_essence") / 2;
        int keep = Inv.count(player, "guardian_essence") - half;
        Inv.remove(player, "guardian_essence", keep);
        use(g.selfTestRift(false), "elemental_rift");
        expect(!Inv.has(player, "guardian_essence", 1) && Inv.count(player, "elemental_guardian_stone") >= half,
                "the elemental guardian made " + Inv.count(player, "elemental_guardian_stone") + " stones from " + half + " essence");
        GuardiansRift.Cell cell = chargedCell();
        expect(cell != null, "the altar didn't charge a cell");
        GuardiansSession.View v0 = g.selfTestView(player);
        use(g.selfTestCellTile(0), "gotr_cell_tile");
        GuardiansSession.View v1 = g.selfTestView(player);
        expect(v1.barrier0().startsWith("Weak") && v1.elemental() - v0.elemental() == cell.placeEnergy,
                "placing a " + cell.label + " cell: barrier " + v1.barrier0() + ", energy +" + (v1.elemental() - v0.elemental()));
        notes.add("a " + cell.label.toLowerCase(java.util.Locale.ROOT) + " cell raised a weak barrier (+" + cell.placeEnergy
                + " energy each)");
        Inv.give(player, "guardian_essence", keep);
        use(g.selfTestRift(true), "catalytic_rift");
        expect(Inv.count(player, "catalytic_guardian_stone") >= keep, "the catalytic guardian made "
                + Inv.count(player, "catalytic_guardian_stone") + " stones");
        Inv.give(player, "chisel", 1);
        GuardiansRift.Cell second = chargedCell();
        expect(second != null, "the catalytic altar didn't charge a cell");
        int riftGuardians = g.selfTestView(player).riftGuardians();
        use(g.selfTestEssencePile(true), "gotr_catalytic_essence");
        expect(g.selfTestView(player).riftGuardians() == riftGuardians + 1, "the essence pile didn't make a rift guardian");
        notes.add("a catalytic rift guardian assembled");

        step = "great guardian";
        double power = g.selfTestView(player).power();
        use(g.selfTestGreatGuardian(), "gotr_great_guardian");
        expect(!Inv.has(player, "elemental_guardian_stone", 1) && !Inv.has(player, "catalytic_guardian_stone", 1)
                && g.selfTestView(player).power() > power, "the Great Guardian took the stones: power " + power + " -> "
                + g.selfTestView(player).power());
        notes.add("powered the Great Guardian to " + Math.round(g.selfTestView(player).power()) + "%");

        step = "portal";
        g.selfTestClock(GuardiansRift.PORTAL_FIRST_TICKS - 1);
        tick();
        tick();
        BlockPos portal = g.selfTestPortal(false);
        expect(SkillingBosses.GOTR_PORTAL.equals(blockId(portal)), "no portal to the huge remains after 160 seconds");
        use(portal, SkillingBosses.GOTR_PORTAL);
        expect(player.getX() < g.selfTestPortal(false).getX() - 2, "the portal didn't take the player to the west shelf");
        BlockPos huge = g.selfTestHugeRemains();
        expect(SkillingBosses.GOTR_HUGE_REMAINS.equals(blockId(huge)), "no huge guardian remains on the shelf");
        BlockHooks.use(player, SkillingBosses.GOTR_HUGE_REMAINS, huge, world.getBlockState(huge));
        expect(until(40, () -> Inv.count(player, "guardian_essence") >= 30), "the huge remains gave "
                + Inv.count(player, "guardian_essence") + " essence in 40 ticks");
        ActionManager.cancel(player);
        BlockPos back = g.selfTestPortal(true);
        BlockHooks.use(player, SkillingBosses.GOTR_PORTAL, back, world.getBlockState(back));
        expect(player.getX() > g.selfTestPortal(true).getX() + 2, "the shelf portal didn't bring the player back");
        notes.add("through the portal: " + Inv.count(player, "guardian_essence") + " essence from the huge remains");

        step = "creatures";
        g.selfTestClock(GuardiansRift.PREP_TICKS + 40);
        until(60, () -> !g.selfTestCreatures().isEmpty());
        List<Mob> creatures = g.selfTestCreatures();
        expect(!creatures.isEmpty(), "no abyssal creatures came out of the rift after the preparation");
        int combatXp = Players.data(player).skills().xpTenths(Skill.ATTACK) + Players.data(player).skills().xpTenths(Skill.HITPOINTS)
                + Players.data(player).skills().xpTenths(Skill.MAGIC);
        int dealt = CombatHandler.applyDamage(player, creatures.get(0), 10);
        int after = Players.data(player).skills().xpTenths(Skill.ATTACK) + Players.data(player).skills().xpTenths(Skill.HITPOINTS)
                + Players.data(player).skills().xpTenths(Skill.MAGIC);
        expect(dealt == 0 && after == combatXp && creatures.get(0).isAlive(), "a direct hit on an abyssal creature dealt " + dealt
                + " and gave " + (after - combatXp) + " XP tenths");
        notes.add(creatures.size() + " abyssal creatures out; they take no damage and give no combat XP");

        step = "seal";
        int el = MinigameData.of(player).stat(player.getUUID(), "gotr_elemental_points");
        int cat = MinigameData.of(player).stat(player.getUUID(), "gotr_catalytic_points");
        // Spend the essence at both altars until the energy qualifies for points, then seal the rift.
        for (int round = 0; round < 6 && g.selfTestView(player).elemental() + g.selfTestView(player).catalytic()
                < com.runecraft.osrs.content.minigames.GuardiansRewards.MIN_ENERGY + 50; round++) {
            if (!Inv.has(player, "guardian_essence", 1)) {
                Inv.give(player, "guardian_essence", 40); // approx: a team's further mining, cut short
            }
            int n = Inv.count(player, "guardian_essence");
            Inv.remove(player, "guardian_essence", n / 2);
            BlockHooks.use(player, "elemental_rift", g.selfTestRift(false), world.getBlockState(g.selfTestRift(false)));
            Inv.give(player, "guardian_essence", n / 2);
            BlockHooks.use(player, "catalytic_rift", g.selfTestRift(true), world.getBlockState(g.selfTestRift(true)));
            BlockHooks.use(player, "gotr_great_guardian", g.selfTestGreatGuardian(), world.getBlockState(g.selfTestGreatGuardian()));
        }
        GuardiansSession.View before = g.selfTestView(player);
        expect(before.elemental() + before.catalytic() >= com.runecraft.osrs.content.minigames.GuardiansRewards.MIN_ENERGY,
                "only " + (before.elemental() + before.catalytic()) + " energy after six altar trips");
        g.selfTestPower(99.9);
        Inv.give(player, "guardian_essence", 5);
        use(g.selfTestRift(false), "elemental_rift");
        use(g.selfTestGreatGuardian(), "gotr_great_guardian");
        expect(g.isEnded(), "powering the Great Guardian to 100% didn't seal the rift (power " + g.selfTestView(player).power() + "%)");
        int gotEl = MinigameData.of(player).stat(player.getUUID(), "gotr_elemental_points") - el;
        int gotCat = MinigameData.of(player).stat(player.getUUID(), "gotr_catalytic_points") - cat;
        expect(gotEl >= 1 && gotCat >= 1, "sealing the rift with " + before.elemental() + "/" + before.catalytic() + " energy gave "
                + gotEl + "/" + gotCat + " points");
        expect(!Inv.has(player, "guardian_essence", 1) && !Inv.has(player, GuardiansRift.UNCHARGED_CELL, 1),
                "game-only items left the temple");
        notes.add("rift sealed: " + gotEl + " elemental and " + gotCat + " catalytic points");

        step = "rewards guardian";
        player.getInventory().clearContent();
        int searches = MinigameData.of(player).stat(player.getUUID(), "gotr_searches");
        GuardiansSession.rewardsGuardian(player);
        expect(MinigameData.of(player).stat(player.getUUID(), "gotr_searches") == searches + 1,
                "searching the Rewards Guardian didn't count a search");
        expect(MinigameData.of(player).stat(player.getUUID(), "gotr_elemental_points") == el + gotEl - 1,
                "a search didn't spend one elemental point");
        notes.add("searched the Rewards Guardian");
        MinigameData.of(player).setStat(player.getUUID(), "gotr_elemental_points", el);
        MinigameData.of(player).setStat(player.getUUID(), "gotr_catalytic_points", cat);
    }

    private GuardiansRift.Cell chargedCell() {
        for (GuardiansRift.Cell c : GuardiansRift.Cell.values()) {
            if (Inv.has(player, c.item, 1)) {
                return c;
            }
        }
        return null;
    }
}
