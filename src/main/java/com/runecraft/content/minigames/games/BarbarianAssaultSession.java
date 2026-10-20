package com.runecraft.content.minigames.games;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.runecraft.action.ActionManager;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.CombatHooks;
import com.runecraft.combat.MonsterStats;
import com.runecraft.combat.PoisonService;
import com.runecraft.content.GameEvents;
import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Rune;
import com.runecraft.osrs.Spell;
import com.runecraft.osrs.content.minigames.BaRules;
import com.runecraft.osrs.content.minigames.BaRules.Call;
import com.runecraft.osrs.content.minigames.BaWaves;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameRules.BarbarianAssault;
import com.runecraft.platform.CommandRegistrationCallback;
import com.runecraft.platform.ServerEntityEvents;
import com.runecraft.platform.ServerPlayConnectionEvents;
import com.runecraft.platform.UseEntityCallback;
import com.runecraft.platform.UseItemCallback;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestApi;
import com.runecraft.quest.QuestScripts;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.util.StackData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Barbarian Assault (OSRS Wiki): a team of up to five, each in a role picked at its entrance (attacker, defender, collector,
 * healer), plays the ten waves of a round in order, starting from the wave the team has reached; the Penance come out of the
 * northern caves by the wiki's wave table, the next of a type each time one dies. Every 30 seconds the calls change: the
 * collector calls the attacker's style, the attacker the egg colour, the healer the defender's bait and the defender the
 * healer's poison, each with their horn. Attackers alone hurt fighters and rangers, and only with the called style (melee
 * stance, Barbarian Assault arrow or spell element; a wrong hit recoils for 2 and costs points); collectors pick up eggs of
 * the called colour into their bag and load the egg launcher (five of each colour), which anyone fires one egg at a time (red
 * 3 + splash, green 1 + poison, blue stun); defenders drop bait to lure runners onto the runner traps (repaired with a hammer
 * and logs) before they escape into the lure cave; healers poison the Penance healers with the called food and heal the team
 * with the vial filled at the spring. The runes, logs and hammer the arena hands out are its own ({@link #LENT_TAG}). Wave 10 ends with the Penance Queen, who only takes omega eggs made by the whole team. Honour points are paid
 * per wave by the wiki's formulas ({@link BaRules#wavePoints}); a death restarts the wave; no combat experience is given.
 *
 * <p>Playing alone or with roles missing (the mod's approach since the start): a role nobody plays is covered by everyone
 * ({@link #covers}): its machine, its tools and its creatures are open to every player, and its calls are known without being
 * made. So a single player, whatever role they chose, does every role's job: fights with the called style, collects and
 * fires eggs, traps runners and poisons healers, and makes the omega eggs alone.
 */
public class BarbarianAssaultSession extends MinigameSession {
    // ------------------------------------------------------------------ arena layout (offsets from the origin; north is -z)
    static final int[] FIGHTER_CAVE = {-12, -20};
    static final int[] HEALER_CAVE = {12, -20};
    /** The east trap (by the runners' cave, the one to use) and the west trap. */
    static final int[][] TRAPS = {{12, -6}, {-12, -6}};
    static final int[] LURE_CAVE = {16, 22};
    /** The west and east egg launchers: both share the hopper; only the west one works on wave 10. */
    static final int[][] LAUNCHERS = {{-10, 6}, {10, 6}};
    static final int[] LOG_PILE = {0, -12};
    static final int[] HAMMER = {2, -12};
    static final int[] SPRING = {12, 18};
    static final int[] QUEEN_TRAPDOOR = {0, -4};
    static final int[] POISON_CRATER = {-18, 0};
    static final int[] PETRIFIED_MUSHROOM = {-18, 8};
    static final int[] LAVA_CRATER = {18, 4};

    /** Item entity tags: an egg the session dropped, and bait dropped while it was (not) the called bait. */
    static final String EGG_TAG = "rcba_egg";
    static final String GOOD_BAIT = "rcba_bait_good";
    static final String BAD_BAIT = "rcba_bait_bad";
    /**
     * Custom-data key on every stack the arena lends (the attacker machine's runes, the log pile's logs, the hammer): OSRS's
     * untradeable arena variants, which vanish outside. They're taken from the pack and the floor whenever the arena's items
     * are, and from a player logging in outside a game.
     */
    static final String LENT_TAG = "rcba_lent";

    /** Items that only exist inside the arena: taken away at the end of each wave (horn, icon and bag when leaving). */
    static final List<String> WAVE_ITEMS = new ArrayList<>();
    static final List<String> KEPT_ITEMS = new ArrayList<>();

    static {
        WAVE_ITEMS.addAll(BaRules.ARROWS);
        WAVE_ITEMS.addAll(BaRules.EGGS);
        WAVE_ITEMS.addAll(List.of(BaRules.YELLOW_EGG, BaRules.POISONED_EGG, BaRules.SPIKED_EGG, BaRules.OMEGA_EGG, "penance_egg"));
        WAVE_ITEMS.addAll(BaRules.BAIT);
        WAVE_ITEMS.addAll(BaRules.POISON_FOOD);
        WAVE_ITEMS.add(BaRules.HEALING_VIAL);
        for (int d = 1; d <= BaRules.VIAL_DOSES; d++) {
            WAVE_ITEMS.add(BaRules.HEALING_VIAL + "_" + d);
        }
        for (String role : BarbarianAssault.ROLES) {
            KEPT_ITEMS.add(role + "_horn");
            KEPT_ITEMS.add(role + "_icon");
        }
        KEPT_ITEMS.add("collection_bag");
    }

    enum Phase { WAITING, RUNNING, QUEEN }

    /** A runner's state (wiki, Penance Runner mechanics): bait it heads for, its 5-tick cycle and whether bad bait confused it. */
    private static final class Runner {
        ItemEntity bait;
        boolean hunting;
        boolean confused;
        long nextCycle;
    }

    /** A Penance healer alternates: chase a player and poison them, wander a moment, chase a runner and heal it. */
    private static final class Healer {
        int mode;
        long until;
        long nextPath;
    }

    /** Poison on a Penance creature: a poisoned food's (4 dropping to 1) or a green egg's (1, five times). */
    private static final class Poison {
        UUID by;
        int hits;
        int interval;
        long next;
    }

    private static final int HEALER_CHASE_PLAYER = 0, HEALER_WANDER = 1, HEALER_CHASE_RUNNER = 2;

    /** True while the session itself deals damage (eggs, poison, traps): the damage gate lets it through. */
    static boolean sessionDamage;
    /** The Barbarian Assault arrow each player fired last (from the arrow entity), for the style check. */
    static final Map<UUID, String> LAST_ARROW = new HashMap<>();

    private final Map<UUID, Integer> roles = new HashMap<>();
    private Phase phase = Phase.WAITING;
    private int wave;
    private int nextWaveIn = 10;
    private long waveStart;
    private final int[] pending = new int[BaWaves.TYPES];
    private final int[] reserve = new int[BaWaves.TYPES];
    private long nextSpawn;
    private final int[] callValue = new int[Call.values().length];
    private final int[] called = new int[Call.values().length];
    private long nextCall;
    private int fighters;
    private int rangers;
    private int runners;
    private int healers;
    private int eggsCollected;
    private int eggsExploded;
    private int hitpointsHealed;
    private int wrongAttacks;
    private int escapes;
    private int wrongPoison;
    /** The hopper both launchers fire from: red, green, blue, omega. */
    private final int[] hopper = new int[4];
    private final int[] trapCharges = {BaRules.TRAP_CHARGES, BaRules.TRAP_CHARGES};
    private long logsBackAt;
    private final Map<UUID, int[]> bags = new HashMap<>();
    private final Map<UUID, Runner> runnerAi = new HashMap<>();
    private final Map<UUID, Healer> healerAi = new HashMap<>();
    /** A poisoned food's poison and a green egg's poison run side by side (OSRS Wiki, Green egg). */
    private final Map<UUID, Poison> foodPoison = new HashMap<>();
    private final Map<UUID, Poison> eggPoison = new HashMap<>();
    private final Map<UUID, Long> stunned = new HashMap<>();
    /** The tick each player can fire their next red, green or blue egg (one at a time, OSRS Wiki). */
    private final Map<UUID, Long> nextFire = new HashMap<>();
    private Mob queen;
    private boolean queenKilled;
    private long queenCheckAt;
    private long queenEmergeAt = -1;
    private long nextVolley;
    private long nextQueenSpawn;
    private long nextYellowEgg;

    public BarbarianAssaultSession(int id, int slot, ServerLevel world, String variant) {
        super(Minigame.BARBARIAN_ASSAULT, id, slot, world, variant);
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
    public boolean awardsXp() {
        // OSRS Wiki: "The minigame does not provide any combat experience for any action."
        return false;
    }

    @Override
    protected void build() {
        room(Blocks.COARSE_DIRT, Blocks.SPRUCE_LOG, Blocks.MUD_BRICKS);
        // The Penance caves in the north wall and the lure cave in the south-east.
        for (int[] cave : new int[][]{FIGHTER_CAVE, HEALER_CAVE}) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = 1; dy <= 3; dy++) {
                    set(cave[0] + dx, dy, -22, Blocks.BLACKSTONE.defaultBlockState());
                }
                set(cave[0] + dx, 0, -21, Blocks.BLACKSTONE.defaultBlockState());
            }
        }
        place(LURE_CAVE[0], LURE_CAVE[1], "ba_lure_cave");
        // Egg launchers on their hills, the traps, the log pile and hammer, the spring and the role machines.
        for (int[] l : LAUNCHERS) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -1; dz <= 2; dz++) {
                    set(l[0] + dx, 0, l[1] + dz, Blocks.PACKED_MUD.defaultBlockState());
                }
            }
            place(l[0], l[1], "ba_egg_launcher");
        }
        for (int[] t : TRAPS) {
            place(t[0], t[1], "ba_runner_trap");
        }
        place(LOG_PILE[0], LOG_PILE[1], "ba_log_pile");
        place(HAMMER[0], HAMMER[1], "ba_hammer");
        place(SPRING[0], SPRING[1], "ba_healer_spring");
        place(-9, 20, "ba_attacker_machine");
        place(-6, 20, "ba_collector_converter");
        place(6, 20, "ba_defender_machine");
        place(9, 20, "ba_healer_machine");
        place(-3, 21, "ba_ladder");
        place(0, 21, "minigame_exit");
        // Wave 10's poison crater, petrified mushroom and lava crater around the edges, and the queen's trapdoor.
        place(POISON_CRATER[0], POISON_CRATER[1], BaRules.POISON_CRATER);
        place(PETRIFIED_MUSHROOM[0], PETRIFIED_MUSHROOM[1], BaRules.PETRIFIED_MUSHROOM);
        place(LAVA_CRATER[0], LAVA_CRATER[1], BaRules.LAVA_CRATER);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                set(QUEEN_TRAPDOOR[0] + dx, 0, QUEEN_TRAPDOOR[1] + dz, Blocks.DARK_OAK_PLANKS.defaultBlockState());
            }
        }
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayer player, int index) {
        return rel(index % 5 - 2, 1, 17);
    }

    // ------------------------------------------------------------------ roles

    int role(ServerPlayer p) {
        return roles.getOrDefault(p.getUUID(), BaRules.ATTACKER);
    }

    private static String roleName(int r) {
        String n = BarbarianAssault.ROLES[r];
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    /** Whether any player in the team plays {@code role}. */
    boolean teamHas(int role) {
        for (ServerPlayer p : online()) {
            if (role(p) == role) {
                return true;
            }
        }
        return false;
    }

    /** Whether {@code p} may do {@code role}'s job: it is their role, or nobody in the team plays it. */
    boolean covers(ServerPlayer p, int role) {
        return role(p) == role || !teamHas(role);
    }

    /** The level that counts for a role's upgrade: the player's own, or the best of the players covering it. */
    private int coveringLevel(int role) {
        int best = 1;
        for (ServerPlayer p : online()) {
            if (covers(p, role)) {
                best = Math.max(best, roleLevel(p, role));
            }
        }
        return best;
    }

    static boolean kandarinHard(ServerPlayer p) {
        return com.runecraft.osrs.content.systems.Diaries.tierComplete(com.runecraft.content.systems.SysState.of(p).diaryDone,
                "kandarin", com.runecraft.osrs.content.systems.Diaries.Tier.HARD);
    }

    @Override
    protected void start() {
        int startWave = BaWaves.WAVES;
        for (ServerPlayer p : online()) {
            String v = MinigameManager.chosenVariant(p);
            int r = BaRules.ATTACKER;
            for (int i = 0; i < BarbarianAssault.ROLES.length; i++) {
                if (v.contains(BarbarianAssault.ROLES[i])) {
                    r = i;
                }
            }
            roles.put(p.getUUID(), r);
            startWave = Math.min(startWave, Math.max(1, waveReached(p)));
        }
        // The waves are played in order: the team starts on the lowest wave any of them has reached this round.
        wave = roles.isEmpty() ? 1 : startWave;
        nextWaveIn = 10;
        for (ServerPlayer p : online()) {
            Msg.game(p, "Your role this game is: " + roleName(role(p)) + ". Your team starts on wave " + wave + ".");
            giveRoleItems(p);
            missingRoles(p);
            // Prayers are disabled in the arena (OSRS Wiki).
            var prayers = Players.data(p).activePrayers();
            if (!prayers.isEmpty()) {
                prayers.clear();
                com.runecraft.network.ServerNetworking.sync(p);
                Msg.game(p, "Your prayers fade: prayers don't work in Barbarian Assault.");
            }
        }
    }

    /** Tells a player which roles nobody plays (they're open to everyone, see {@link #covers}). */
    private void missingRoles(ServerPlayer p) {
        List<String> missing = new ArrayList<>();
        for (int r = 0; r < BarbarianAssault.ROLES.length; r++) {
            if (!teamHas(r)) {
                missing.add(BarbarianAssault.ROLES[r]);
            }
        }
        if (!missing.isEmpty()) {
            Msg.game(p, "Nobody plays " + String.join(", ", missing) + ": those jobs are yours too, and their calls come to you by "
                    + "themselves.");
        }
    }

    private void giveRoleItems(ServerPlayer p) {
        String role = BarbarianAssault.ROLES[role(p)];
        for (String item : new String[]{role + "_horn", role + "_icon"}) {
            if (!Inv.has(p, item, 1)) {
                Inv.give(p, item, 1);
            }
        }
        if (covers(p, BaRules.COLLECTOR) && !Inv.has(p, "collection_bag", 1)) {
            Inv.give(p, "collection_bag", 1);
        }
    }

    // ------------------------------------------------------------------ the game loop

    @Override
    protected void gameTick() {
        if (phase == Phase.WAITING) {
            if (--nextWaveIn <= 0) {
                startWave();
            }
            return;
        }
        tagBait();
        if (phase == Phase.RUNNING && ticks >= nextCall) {
            rollCalls();
        }
        spawnPenance();
        runners();
        healers();
        poison();
        stuns();
        pickUpEggs();
        if (wave >= BaWaves.WAVES) {
            queenWave();
        } else if (allOut() && penanceAlive() == 0) {
            completeWave();
        }
    }

    void startWave() {
        phase = Phase.RUNNING;
        waveStart = ticks;
        fighters = rangers = runners = healers = 0;
        eggsCollected = eggsExploded = hitpointsHealed = wrongAttacks = escapes = wrongPoison = 0;
        java.util.Arrays.fill(hopper, 0);
        java.util.Arrays.fill(trapCharges, BaRules.TRAP_CHARGES);
        logsBackAt = ticks;
        bags.clear();
        runnerAi.clear();
        healerAi.clear();
        foodPoison.clear();
        eggPoison.clear();
        stunned.clear();
        nextFire.clear();
        clearFloor();
        for (int t = 0; t < BaWaves.TYPES; t++) {
            pending[t] = BaWaves.initial(wave, t);
            reserve[t] = BaWaves.reserve(wave, t);
        }
        nextSpawn = ticks;
        queen = null;
        queenEmergeAt = -1;
        queenCheckAt = ticks + BaWaves.QUEEN_CHECK_TICKS;
        rollCalls();
        broadcastRed("Wave " + wave + " begins!");
        for (ServerPlayer p : online()) {
            giveRoleItems(p);
        }
    }

    /** New calls every 30 seconds; nobody has called them yet. */
    void rollCalls() {
        for (Call c : Call.values()) {
            callValue[c.ordinal()] = random.nextInt(c.options().size());
            called[c.ordinal()] = -1;
        }
        nextCall = ticks + BaWaves.CALL_TICKS;
        for (ServerPlayer p : online()) {
            Call make = Call.madeBy(role(p));
            Msg.action(p, "New call! Call " + make.label(callValue[make.ordinal()]) + " to your " + roleName(make.receiver()) + ".");
        }
    }

    /** Every Penance of the wave has come out (nothing pending or in reserve). */
    boolean allOut() {
        for (int t = 0; t < BaWaves.TYPES; t++) {
            if (pending[t] > 0 || reserve[t] > 0) {
                return false;
            }
        }
        return true;
    }

    int penanceAlive() {
        int n = 0;
        for (String id : BarbarianAssault.PENANCE) {
            n += living(id).size();
        }
        return n;
    }

    private static int typeOf(String creatureId) {
        for (int t = 0; t < BarbarianAssault.PENANCE.length; t++) {
            if (BarbarianAssault.PENANCE[t].equals(creatureId)) {
                return t;
            }
        }
        return -1;
    }

    /** One Penance of each type with some still to come every 6 seconds, from their caves. */
    private void spawnPenance() {
        if (ticks < nextSpawn) {
            return;
        }
        nextSpawn = ticks + BaWaves.SPAWN_INTERVAL_TICKS;
        for (int t = 0; t < BaWaves.TYPES; t++) {
            if (pending[t] > 0) {
                pending[t]--;
                spawnOne(t);
            }
        }
    }

    Mob spawnOne(int type) {
        int[] cave = type <= BaWaves.RANGER ? FIGHTER_CAVE : HEALER_CAVE;
        Mob mob = spawn(BarbarianAssault.PENANCE[type], randomFloor(cave[0], cave[1], 2));
        if (mob == null) {
            return null;
        }
        BaWaves.Stats st = BaWaves.stats(wave, type);
        if (mob instanceof OsrsMonsterEntity om) {
            om.overrideStats(s -> new MonsterStats(s.name(), st.combat(), st.hitpoints(), st.attack(), st.strength(), st.defence(),
                    s.magic(), st.ranged(), s.defenceBonus(), s.attackSpeed(), s.style(), Math.max(1, st.maxHit()), s.monster(),
                    s.profile(), s.boss()));
            if (type == BaWaves.FIGHTER || type == BaWaves.RANGER) {
                // Each wave's max hit (wave 1 fighter 4 ... wave 9 ranger 9), scaling the creature's one attack.
                om.attackInterceptor = (m, target, attack) -> {
                    if (attack == null || attack.maxHit() <= 0) {
                        return false;
                    }
                    com.runecraft.boss.Mechanics.perform(m, target, attack, st.maxHit() / (double) attack.maxHit());
                    return true;
                };
            }
        }
        setHitpoints(mob, st.hitpoints());
        if (type == BaWaves.FIGHTER || type == BaWaves.RANGER) {
            aggro(mob);
        } else if (type == BaWaves.RUNNER) {
            runnerAi.put(mob.getUUID(), new Runner());
        } else {
            healerAi.put(mob.getUUID(), new Healer());
        }
        return mob;
    }

    @Override
    protected void onKill(ServerPlayer player, LivingEntity entity, String creatureId) {
        int type = typeOf(creatureId);
        if (type >= 0) {
            switch (type) {
                case BaWaves.FIGHTER -> fighters++;
                case BaWaves.RANGER -> rangers++;
                case BaWaves.RUNNER -> runners++;
                default -> healers++;
            }
            dropEggs(entity.position());
            if (reserve[type] > 0) {
                reserve[type]--;
                pending[type]++;
            }
            runnerAi.remove(entity.getUUID());
            healerAi.remove(entity.getUUID());
            foodPoison.remove(entity.getUUID());
            eggPoison.remove(entity.getUUID());
            stunned.remove(entity.getUUID());
        } else if (creatureId.equals("penance_queen")) {
            queenKilled = true;
            broadcastRed("The Penance Queen has been defeated!");
            end(true);
        }
    }

    /** Every Penance drops two red, two green and two blue eggs (OSRS Wiki drop tables). */
    private void dropEggs(Vec3 at) {
        for (String egg : BaRules.EGGS) {
            dropEgg(egg, BaRules.EGGS_PER_COLOUR, at.add(random.nextDouble() - 0.5, 0.2, random.nextDouble() - 0.5));
        }
    }

    /** Eggs don't stack (OSRS), so each lies on the floor on its own. */
    private void dropEgg(String egg, int count, Vec3 at) {
        for (int i = 0; i < count; i++) {
            ItemStack stack = Inv.stack(egg, 1);
            if (stack.isEmpty()) {
                return;
            }
            ItemEntity item = new ItemEntity(world, at.x + i * 0.2, at.y, at.z, stack);
            item.setNeverPickUp();
            item.setDeltaMovement(Vec3.ZERO);
            item.addTag(EGG_TAG);
            world.addFreshEntity(item);
        }
    }

    private void completeWave() {
        BaRules.WaveStats st = waveStats();
        for (ServerPlayer p : online()) {
            int r = role(p);
            int honour = giveHonour(p, r, BaRules.wavePoints(r, wave, st, kandarinHard(p)));
            Msg.game(p, "Wave " + wave + " complete! You earn " + honour + " " + roleName(r) + " honour points ("
                    + fighters + " fighters, " + rangers + " rangers, " + runners + " runners, " + healers + " healers killed"
                    + (eggsCollected > 0 ? ", " + eggsCollected + " eggs" : "") + (escapes > 0 ? ", " + escapes + " escaped" : "")
                    + (wrongAttacks > 0 ? ", " + wrongAttacks + " wrong attacks" : "") + (wrongPoison > 0 ? ", " + wrongPoison
                    + " wrong poison" : "") + ").");
            // The wave reached this round unlocks the low (wave 3) and medium (wave 6) gambles.
            if (waveReached(p) < wave + 1) {
                MinigameData.of(p).setStat(p.getUUID(), WAVE_STAT, wave + 1);
            }
            GameEvents.fireActivity(p, "ba_wave");
            offerLevel(p, r);
            stripItems(p, false);
        }
        clearFloor();
        for (LivingEntity e : living(null)) {
            e.discard();
        }
        wave++;
        phase = Phase.WAITING;
        nextWaveIn = BaWaves.NEXT_WAVE_TICKS;
        broadcast("Wave " + wave + " starts in 60 seconds. The leader can climb the ladder to start it now.");
    }

    BaRules.WaveStats waveStats() {
        return new BaRules.WaveStats(fighters, rangers, runners, healers, eggsCollected, eggsExploded, hitpointsHealed,
                wrongAttacks, escapes, wrongPoison);
    }

    /** After a wave, a player who can afford their role's next level is offered it (OSRS's reward screen). */
    private static void offerLevel(ServerPlayer p, int role) {
        int level = roleLevel(p, role);
        if (level >= BarbarianAssault.MAX_ROLE_LEVEL) {
            return;
        }
        int cost = BarbarianAssault.levelCost(level + 1);
        if (MinigameData.of(p).points(p.getUUID(), "ba_" + BarbarianAssault.ROLES[role]) >= cost) {
            String command = "/minigame balevel " + BarbarianAssault.ROLES[role];
            p.displayClientMessage(Component.literal("You can afford " + roleName(role) + " level " + (level + 1) + ". ")
                    .withStyle(ChatFormatting.GOLD).append(Component.literal("[Level up: " + cost + "]").withStyle(st -> st
                            .withColor(ChatFormatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)))),
                    false);
        }
    }

    /** A death, or a teammate leaving, restarts the wave for the whole team (OSRS Wiki). */
    void restartWave(String why) {
        broadcastRed(why + " The wave must be restarted.");
        for (LivingEntity e : living(null)) {
            e.discard();
        }
        clearFloor();
        queen = null;
        int i = 0;
        for (ServerPlayer p : online()) {
            stripItems(p, false);
            p.setHealth(p.getMaxHealth());
            BlockPos at = spawnPoint(p, i++);
            p.teleportTo(world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p.getYRot(), p.getXRot());
        }
        phase = Phase.WAITING;
        nextWaveIn = 20; // approx: the team starts the wave again from the waiting room
    }

    @Override
    protected void onDeath(ServerPlayer player) {
        Msg.red(player, "Oh dear, you are dead!");
        player.setHealth(player.getMaxHealth());
        PoisonService.cure(player, true, 0, ActionManager.gameTick());
        if (phase != Phase.WAITING) {
            restartWave(player.getName().getString() + " has died!");
        }
    }

    @Override
    protected void onLeave(ServerPlayer player) {
        stripItems(player, true);
        bags.remove(player.getUUID());
        LAST_ARROW.remove(player.getUUID());
        roles.remove(player.getUUID());
        if (!isEnded() && phase != Phase.WAITING && playerCount() > 0) {
            restartWave(player.getName().getString() + " has left the team.");
        }
    }

    @Override
    protected void onEnd(boolean success) {
        // The arena's eggs and bait stay with the arena (they'd otherwise be handed to the first player and taken back).
        clearFloor();
        if (!success || !queenKilled) {
            return;
        }
        for (ServerPlayer p : online()) {
            int r = role(p);
            // Wave 10: 80 points in your role and 5 in each of the others, no bonuses or penalties.
            boolean diary = kandarinHard(p);
            for (int other = 0; other < BarbarianAssault.ROLES.length; other++) {
                giveHonour(p, other, BaRules.wave10(r, other, diary));
            }
            int kc = MinigameData.of(p).incrementStat(p.getUUID(), "kc:" + game.id());
            String queenKey = com.runecraft.osrs.content.minigames.MinigameCurrencies.key(
                    com.runecraft.osrs.content.minigames.MinigameCurrencies.BA_QUEEN);
            if (MinigameData.of(p).points(p.getUUID(), queenKey) < BarbarianAssault.MAX_QUEEN_KILLS) {
                addPoints(p, com.runecraft.osrs.content.minigames.MinigameCurrencies.BA_QUEEN, 1);
            }
            Msg.game(p, "Congratulations! You've defeated the Penance Queen. Queen kills: " + kc + ". You earn "
                    + BaRules.wave10(r, r, diary) + " " + roleName(r) + " honour points and " + BaRules.wave10(r, (r + 1) % 4, diary)
                    + " in each other role.");
            // The pet penance queen comes from high gambles (BaGambles), not from the kill itself (OSRS Wiki).
            // Finishing wave 10 resets the round, so low and medium gambles need new waves.
            MinigameData.of(p).setStat(p.getUUID(), WAVE_STAT, 0);
            GameEvents.fireActivity(p, "ba_wave");
            offerLevel(p, r);
        }
    }

    /** Adds honour points in a role, capped at 5,000; returns the amount added. */
    private int giveHonour(ServerPlayer p, int role, int amount) {
        String key = "ba_" + BarbarianAssault.ROLES[role];
        int add = BarbarianAssault.addHonour(MinigameData.of(p).points(p.getUUID(), key), amount);
        addPoints(p, "pts:" + key, add);
        return add;
    }

    // ------------------------------------------------------------------ items handed out and taken back

    /** Whether a stack is one the arena lent ({@link #LENT_TAG}). */
    static boolean isLent(ItemStack stack) {
        return !stack.isEmpty() && StackData.contains(stack, LENT_TAG);
    }

    /** How many of {@code item} the player holds in lent stacks. */
    static int lentCount(ServerPlayer p, String item) {
        int n = 0;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (isLent(s) && Inv.idOf(s).equals(item)) {
                n += s.getCount();
            }
        }
        return n;
    }

    /**
     * Hands out arena items marked {@link #LENT_TAG}: spells and repairs still use them up (they match on the item alone), but
     * they never leave the arena. The lent stacks are moved ahead of the player's own of that item, so they're used first.
     */
    void lend(ServerPlayer p, String item, int count) {
        net.minecraft.world.item.Item it = Inv.item(item);
        for (int left = count; left > 0; ) {
            int n = Math.min(left, it.getDefaultMaxStackSize());
            ItemStack stack = Inv.stack(item, n);
            if (stack.isEmpty()) {
                return;
            }
            StackData.update(stack, tag -> tag.putBoolean(LENT_TAG, true));
            Inv.give(p, stack);
            left -= n;
        }
        var inv = p.getInventory();
        List<Integer> slots = new ArrayList<>();
        List<ItemStack> order = new ArrayList<>();
        List<ItemStack> own = new ArrayList<>();
        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack s = inv.items.get(i);
            if (s.is(it)) {
                slots.add(i);
                (isLent(s) ? order : own).add(s);
            }
        }
        order.addAll(own);
        for (int k = 0; k < slots.size(); k++) {
            inv.setItem(slots.get(k), order.get(k));
        }
    }

    /** Uses up one of {@code item}, a lent one first. */
    private static void useLentFirst(ServerPlayer p, String item) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (isLent(s) && Inv.idOf(s).equals(item)) {
                s.shrink(1);
                return;
            }
        }
        Inv.remove(p, item, 1);
    }

    /** Removes every lent stack from a player's pack, armour and offhand; returns how many items. */
    static int stripLent(ServerPlayer p) {
        int n = 0;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (isLent(s)) {
                n += s.getCount();
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        return n;
    }

    /** Takes the arena's items back: the wave's supplies and every lent stack (and the horn, icon and bag when {@code all}). */
    void stripItems(ServerPlayer p, boolean all) {
        for (String item : WAVE_ITEMS) {
            Inv.remove(p, item, 10_000);
        }
        if (all) {
            for (String item : KEPT_ITEMS) {
                Inv.remove(p, item, 10_000);
            }
        }
        stripLent(p);
        bags.remove(p.getUUID());
    }

    /** Removes the eggs, bait and lent items (runes, logs, the hammer) lying in the arena. */
    private void clearFloor() {
        for (ItemEntity e : world.getEntitiesOfClass(ItemEntity.class, box(), ItemEntity::isAlive)) {
            String id = Inv.idOf(e.getItem());
            if (WAVE_ITEMS.contains(id) || e.getTags().contains(EGG_TAG) || isLent(e.getItem())) {
                e.discard();
            }
        }
    }

    // ------------------------------------------------------------------ calls

    /** The value a player was told for the call they act on: what their partner called, or the real call if nobody plays it. */
    int told(ServerPlayer p) {
        Call c = Call.heardBy(role(p));
        return teamHas(c.caller()) ? called[c.ordinal()] : callValue[c.ordinal()];
    }

    int callValue(Call c) {
        return callValue[c.ordinal()];
    }

    /** A player calls with their horn ("Tell-red", "Tell-controlled", ...); the partner hears whatever they called. */
    void call(ServerPlayer p, String word) {
        if (phase != Phase.RUNNING) {
            Msg.game(p, "There's nothing to call right now.");
            return;
        }
        Call c = Call.madeBy(role(p));
        int option = c.index(word);
        if (option < 0) {
            Msg.game(p, "Your horn can't call that.");
            return;
        }
        called[c.ordinal()] = option;
        Msg.game(p, "You call: " + c.label(option) + ".");
        for (ServerPlayer q : online()) {
            if (q != p && role(q) == c.receiver()) {
                Msg.action(q, p.getName().getString() + " calls: " + c.label(option));
            }
        }
    }

    private void medic(ServerPlayer p) {
        for (ServerPlayer q : online()) {
            if (covers(q, BaRules.HEALER) && q != p) {
                q.displayClientMessage(Component.literal(p.getName().getString() + " needs healing! ("
                        + (int) p.getHealth() + " hitpoints)").withStyle(ChatFormatting.RED), false);
            }
        }
        Msg.game(p, "You call for a healer.");
    }

    /** The horn's options, as clickable chat lines. */
    private void hornMenu(ServerPlayer p) {
        Call c = Call.madeBy(role(p));
        MutableComponent line = Component.literal("Call to your " + roleName(c.receiver()) + ": ").withStyle(ChatFormatting.GOLD);
        for (String option : c.options()) {
            line.append(link("[Tell-" + option + "] ", "/ba call " + option));
        }
        line.append(link("[Medic]", "/ba call medic"));
        p.displayClientMessage(line, false);
    }

    private static MutableComponent link(String text, String command) {
        return Component.literal(text).withStyle(st -> st.withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)));
    }

    // ------------------------------------------------------------------ attacker

    /**
     * A player's hit on a Penance after its damage roll: fighters and rangers take damage only from the attacker's called style
     * (+1 to +5 bonus damage by attacker level, even on a miss); a wrong style deals nothing and recoils 2 on the attacker.
     */
    int attackRoll(ServerPlayer player, LivingEntity target, AttackType type, int damage) {
        String id = MinigameManager.creatureId(target);
        int t = typeOf(id);
        if (t != BaWaves.FIGHTER && t != BaWaves.RANGER) {
            return damage;
        }
        if (!covers(player, BaRules.ATTACKER)) {
            Msg.action(player, "Only the attacker can fight the Penance " + (t == BaWaves.FIGHTER ? "fighters." : "rangers."));
            return 0;
        }
        int style = styleOf(player, type);
        if (style == -2) {
            Msg.action(player, "Those arrows can't harm the Penance: take arrows from the attacker item machine.");
            return 0;
        }
        if (style != callValue[Call.STYLE.ordinal()]) {
            if (wave < BaWaves.WAVES) {
                wrongAttacks++;
            }
            Msg.red(player, "That's the wrong attack style! The Penance call is " + Call.STYLE.label(callValue[Call.STYLE.ordinal()]) + ".");
            hurt(player, BaRules.WRONG_STYLE_RECOIL);
            return 0;
        }
        return damage + BaRules.attackerBonus(roleLevel(player, BaRules.ATTACKER));
    }

    /** The style call an attack answers: melee stance, Barbarian Assault arrow or spell element; -2 for other arrows, -1 else. */
    static int styleOf(ServerPlayer player, AttackType type) {
        var data = Players.data(player);
        if (type == AttackType.RANGED) {
            String arrow = LAST_ARROW.get(player.getUUID());
            if (arrow == null) {
                arrow = Inv.idOf(player.getProjectile(player.getMainHandItem()));
            }
            int s = BaRules.arrowStyle(arrow);
            return s >= 0 ? s : -2;
        }
        if (type == AttackType.MAGIC) {
            String weapon = Inv.idOf(player.getMainHandItem());
            if (com.runecraft.osrs.CombatBonuses.poweredStaffMaxHit(weapon, data.skills().currentLevel(com.runecraft.osrs.Skill.MAGIC)) >= 0) {
                return -1; // only standard spellbook spells count
            }
            Spell spell = data.autocast();
            return spell == null ? -1 : BaRules.spellStyle(spell.element());
        }
        return BaRules.meleeStyle(data.style().forMelee().displayName());
    }

    /**
     * The damage gate (every damage path): the queen takes only omega eggs, runners and Penance healers only the session's own
     * damage (eggs, poison, traps), fighters and rangers only the attacker (or anyone when nobody plays attacker), and a
     * stunned Penance nothing at all.
     */
    String deny(ServerPlayer player, LivingEntity target) {
        if (stunned.containsKey(target.getUUID())) {
            return "It's stunned: it can't be hurt until it recovers.";
        }
        if (sessionDamage) {
            return null;
        }
        String id = MinigameManager.creatureId(target);
        if (id.equals("penance_queen")) {
            return "The Penance Queen can only be harmed by omega eggs fired from the egg launcher.";
        }
        int t = typeOf(id);
        if (t == BaWaves.RUNNER) {
            return "You can't attack the runners: lure them onto a runner trap with bait.";
        }
        if (t == BaWaves.HEALER) {
            return "The Penance healers can only be hurt with poisoned food.";
        }
        if ((t == BaWaves.FIGHTER || t == BaWaves.RANGER) && !covers(player, BaRules.ATTACKER)) {
            return "Only the attacker can fight that.";
        }
        return null;
    }

    /** Damage the session deals itself (eggs, poison, traps), credited to a player so kills count. */
    private void sessionHit(ServerPlayer by, LivingEntity target, int damage) {
        ServerPlayer credit = by != null && has(by) ? by : first();
        if (credit == null || damage <= 0) {
            return;
        }
        sessionDamage = true;
        try {
            CombatHandler.applyDamage(credit, target, damage);
        } finally {
            sessionDamage = false;
        }
    }

    private ServerPlayer first() {
        List<ServerPlayer> on = online();
        return on.isEmpty() ? null : on.get(0);
    }

    // ------------------------------------------------------------------ collector

    int bagCapacity() {
        return BarbarianAssault.collectorBag(coveringLevel(BaRules.COLLECTOR));
    }

    int[] bag(ServerPlayer p) {
        return bags.computeIfAbsent(p.getUUID(), k -> new int[3]);
    }

    private void pickUpEggs() {
        for (ItemEntity item : world.getEntitiesOfClass(ItemEntity.class, box(), e -> e.isAlive() && e.getTags().contains(EGG_TAG))) {
            for (ServerPlayer p : online()) {
                if (!covers(p, BaRules.COLLECTOR)) {
                    continue; // someone else standing on the egg doesn't stop the collector taking it
                }
                double dx = p.getX() - item.getX();
                double dz = p.getZ() - item.getZ();
                if (dx * dx + dz * dz < 1.6 * 1.6 && Math.abs(p.getY() - item.getY()) < 2) {
                    pickUp(p, item);
                    if (!item.isAlive()) {
                        break;
                    }
                }
            }
        }
    }

    /**
     * A collector walking over an egg: the called colour (or a yellow egg, any time) goes into the collection bag, then the pack;
     * with no room it explodes. Another colour stays put unless the collector deliberately takes it (sneaking), when it explodes.
     */
    void pickUp(ServerPlayer p, ItemEntity item) {
        if (!covers(p, BaRules.COLLECTOR)) {
            return;
        }
        String egg = Inv.idOf(item.getItem());
        boolean yellow = egg.equals(BaRules.YELLOW_EGG);
        int colour = BaRules.eggColour(egg);
        if (!yellow && colour != callValue[Call.EGG.ordinal()]) {
            if (p.isShiftKeyDown()) {
                takeOne(item);
                explode(p);
            }
            return;
        }
        int[] bag = bag(p);
        if (!yellow && bag[0] + bag[1] + bag[2] < bagCapacity()) {
            bag[colour]++;
        } else if (p.getInventory().getFreeSlot() >= 0) {
            Inv.give(p, egg, 1);
        } else {
            takeOne(item);
            explode(p);
            return;
        }
        takeOne(item);
        if (!yellow && wave < BaWaves.WAVES) {
            eggsCollected++;
        }
    }

    private static void takeOne(ItemEntity item) {
        ItemStack stack = item.getItem().copy();
        stack.shrink(1);
        if (stack.isEmpty()) {
            item.discard();
        } else {
            item.setItem(stack);
        }
    }

    private void explode(ServerPlayer p) {
        if (wave < BaWaves.WAVES) {
            eggsExploded++;
        }
        world.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY() + 0.5, p.getZ(), 1, 0, 0, 0, 0);
        Msg.red(p, "The egg explodes!");
        hurt(p, BaRules.EXPLODE_DAMAGE);
    }

    /**
     * The collector converter: with collector level 2+, each egg in the pack has the level's chance to change colour (green to
     * red, red and blue to green) and is destroyed otherwise (OSRS Wiki, Collector Converter).
     */
    void convert(ServerPlayer p) {
        int level = roleLevel(p, BaRules.COLLECTOR);
        if (level < 2) {
            Msg.game(p, "You need to be a level 2 collector to use the converter.");
            return;
        }
        int[] made = new int[BaRules.EGGS.size()];
        int changed = 0;
        int destroyed = 0;
        for (String egg : BaRules.EGGS) {
            int n = Inv.remove(p, egg, 10_000);
            for (int i = 0; i < n; i++) {
                if (random.nextInt(100) < BaRules.convertPercent(level)) {
                    made[BaRules.eggColour(BaRules.converted(egg))]++;
                    changed++;
                } else {
                    destroyed++;
                }
            }
        }
        for (int c = 0; c < made.length; c++) {
            if (made[c] > 0) {
                Inv.give(p, BaRules.EGGS.get(c), made[c]);
            }
        }
        if (changed + destroyed == 0) {
            Msg.game(p, "You have no eggs to convert.");
            return;
        }
        Msg.game(p, changed + " egg" + (changed == 1 ? "" : "s") + " change colour" + (destroyed > 0 ? "; " + destroyed
                + " " + (destroyed == 1 ? "is" : "are") + " destroyed." : "."));
    }

    // ------------------------------------------------------------------ the egg launcher

    private int launcherAt(BlockPos pos) {
        BlockPos r = pos.subtract(origin);
        for (int i = 0; i < LAUNCHERS.length; i++) {
            if (r.getX() == LAUNCHERS[i][0] && r.getZ() == LAUNCHERS[i][1]) {
                return i;
            }
        }
        return 0;
    }

    private void launcher(ServerPlayer p, int which) {
        if (wave >= BaWaves.WAVES && which == 1) {
            Msg.game(p, "There's no egg launcher on this side during the queen wave: use the west one.");
            return;
        }
        if (covers(p, BaRules.COLLECTOR) && load(p) > 0) {
            return;
        }
        fireMenu(p);
    }

    /**
     * The collector loads the eggs in the bag and then the pack into the hopper, up to five of each colour (the rest stay where
     * they were), and every omega egg; returns how many.
     */
    int load(ServerPlayer p) {
        int n = 0;
        boolean full = false;
        int[] bag = bag(p);
        for (int c = 0; c < 3; c++) {
            int room = Math.max(0, BaRules.HOPPER_MAX_PER_COLOUR - hopper[c]);
            int fromBag = Math.min(room, bag[c]);
            bag[c] -= fromBag;
            int fromPack = Inv.remove(p, BaRules.EGGS.get(c), room - fromBag);
            hopper[c] += fromBag + fromPack;
            n += fromBag + fromPack;
            full |= bag[c] > 0 || Inv.has(p, BaRules.EGGS.get(c), 1);
        }
        int omega = Inv.remove(p, BaRules.OMEGA_EGG, 10_000);
        hopper[3] += omega;
        n += omega;
        if (n > 0) {
            Msg.game(p, "You load " + n + " egg" + (n == 1 ? "" : "s") + " into the egg hopper. (" + hopperText() + ")");
        }
        if (full) {
            Msg.game(p, "The hopper holds at most " + BaRules.HOPPER_MAX_PER_COLOUR + " eggs of each colour.");
        }
        return n;
    }

    private String hopperText() {
        return "red " + hopper[0] + ", green " + hopper[1] + ", blue " + hopper[2] + (wave >= BaWaves.WAVES ? ", omega " + hopper[3] : "");
    }

    private static final String[] COLOURS = {"red", "green", "blue", "omega"};
    private static final String[] TARGETS = {"fighter", "ranger", "runner", "healer"};

    private void fireMenu(ServerPlayer p) {
        if (hopper[0] + hopper[1] + hopper[2] + hopper[3] == 0) {
            Msg.game(p, "The egg hopper is empty: the collector loads it with eggs.");
            return;
        }
        p.displayClientMessage(Component.literal("Egg launcher (" + hopperText() + "):").withStyle(ChatFormatting.GOLD), false);
        for (int c = 0; c < 3; c++) {
            if (hopper[c] == 0) {
                continue;
            }
            MutableComponent line = Component.literal(" " + COLOURS[c] + " egg at: ").withStyle(ChatFormatting.GRAY);
            for (int t = 0; t < TARGETS.length; t++) {
                if (!living(BarbarianAssault.PENANCE[t]).isEmpty()) {
                    line.append(link("[" + TARGETS[t] + "] ", "/ba fire " + COLOURS[c] + " " + TARGETS[t]));
                }
            }
            p.displayClientMessage(line, false);
        }
        if (hopper[3] > 0 && queen != null && queen.isAlive()) {
            p.displayClientMessage(Component.literal(" omega egg at: ").withStyle(ChatFormatting.GRAY)
                    .append(link("[the Penance Queen]", "/ba fire omega queen")), false);
        }
    }

    /** Fires one egg of a colour at the nearest Penance of a type ("queen" for omega eggs). */
    void fire(ServerPlayer p, String colour, String target) {
        int c = java.util.Arrays.asList(COLOURS).indexOf(colour);
        int launcher = nearLauncher(p);
        if (c < 0 || launcher < 0) {
            Msg.game(p, "You need to stand at an egg launcher.");
            return;
        }
        if (wave >= BaWaves.WAVES && launcher == 1) {
            Msg.game(p, "There's no egg launcher on this side during the queen wave: use the west one.");
            return;
        }
        if (hopper[c] <= 0) {
            Msg.game(p, "There are no " + colour + " eggs in the hopper.");
            return;
        }
        if (c == 3) {
            if (queen == null || !queen.isAlive()) {
                Msg.game(p, "There's nothing to fire an omega egg at.");
                return;
            }
            if (!allOut() || penanceAlive() > 0) {
                Msg.game(p, "Every other Penance must be dead before the omega eggs can hurt the queen.");
                return;
            }
            // Omega eggs have no firing delay (OSRS Wiki, Egg launcher) and hit for 30-39.
            hopper[3]--;
            Msg.game(p, "You fire an omega egg at the Penance Queen!");
            world.sendParticles(ParticleTypes.EXPLOSION, queen.getX(), queen.getY() + 1, queen.getZ(), 3, 1, 1, 1, 0);
            sessionHit(p, queen, BaRules.omegaEggDamage(random.nextDouble()));
            return;
        }
        if (ticks < nextFire.getOrDefault(p.getUUID(), 0L)) {
            Msg.game(p, "You can only fire one egg at a time.");
            return;
        }
        int t = java.util.Arrays.asList(TARGETS).indexOf(target);
        LivingEntity mob = t < 0 ? null : nearest(living(BarbarianAssault.PENANCE[t]), rel(LAUNCHERS[launcher][0], 1, LAUNCHERS[launcher][1]));
        if (mob == null) {
            Msg.game(p, "There's no Penance " + target + " to fire at.");
            return;
        }
        hopper[c]--;
        nextFire.put(p.getUUID(), ticks + BaRules.EGG_FIRE_DELAY_TICKS);
        launch(p, c, mob);
    }

    private int nearLauncher(ServerPlayer p) {
        for (int i = 0; i < LAUNCHERS.length; i++) {
            if (p.blockPosition().distSqr(rel(LAUNCHERS[i][0], 1, LAUNCHERS[i][1])) <= 5 * 5) {
                return i;
            }
        }
        return -1;
    }

    /** An egg hitting a Penance: red 3 (+0-1 to others within 3), green 1 and poison, blue stun (OSRS Wiki, Egg launcher). */
    void launch(ServerPlayer p, int colour, LivingEntity mob) {
        if (stunned.containsKey(mob.getUUID())) {
            Msg.game(p, "It's stunned: the egg does nothing.");
            return;
        }
        switch (colour) {
            case 0 -> {
                world.sendParticles(ParticleTypes.EXPLOSION, mob.getX(), mob.getY() + 1, mob.getZ(), 1, 0, 0, 0, 0);
                List<LivingEntity> near = new ArrayList<>();
                for (String id : BarbarianAssault.PENANCE) {
                    for (LivingEntity o : living(id)) {
                        if (o != mob && o.distanceToSqr(mob) <= BaRules.RED_SPLASH_RANGE * BaRules.RED_SPLASH_RANGE) {
                            near.add(o);
                        }
                    }
                }
                sessionHit(p, mob, BaRules.RED_DAMAGE);
                for (LivingEntity o : near) {
                    sessionHit(p, o, random.nextInt(BaRules.RED_SPLASH_MAX + 1));
                }
            }
            case 1 -> {
                world.sendParticles(ParticleTypes.ITEM_SLIME, mob.getX(), mob.getY() + 1, mob.getZ(), 8, 0.3, 0.3, 0.3, 0);
                sessionHit(p, mob, BaRules.GREEN_DAMAGE);
                if (mob.isAlive()) {
                    // Beside any food poison, never resetting or delaying it; every 18 s on a Penance healer, 3 s on the others.
                    Poison ps = new Poison();
                    ps.by = p.getUUID();
                    ps.interval = BaRules.greenPoisonInterval(typeOf(MinigameManager.creatureId(mob)) == BaWaves.HEALER);
                    ps.next = ticks + ps.interval;
                    eggPoison.put(mob.getUUID(), ps);
                }
            }
            default -> {
                world.sendParticles(ParticleTypes.ELECTRIC_SPARK, mob.getX(), mob.getY() + 1, mob.getZ(), 10, 0.3, 0.5, 0.3, 0);
                stunned.put(mob.getUUID(), ticks + BaRules.BLUE_STUN_TICKS);
                if (mob instanceof Mob m) {
                    m.setNoAi(true);
                }
            }
        }
        Msg.game(p, "You fire a " + COLOURS[colour] + " egg at the Penance " + MinigameManager.creatureId(mob).substring(8) + ".");
    }

    private static LivingEntity nearest(List<LivingEntity> list, BlockPos from) {
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (LivingEntity e : list) {
            double d = e.blockPosition().distSqr(from);
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    private void stuns() {
        stunned.entrySet().removeIf(e -> {
            if (ticks < e.getValue()) {
                return false;
            }
            for (LivingEntity m : living(null)) {
                if (m.getUUID().equals(e.getKey()) && m instanceof Mob mob) {
                    mob.setNoAi(false);
                }
            }
            return true;
        });
    }

    // ------------------------------------------------------------------ defender

    /** Tags bait dropped in the arena as good (the called bait when dropped) or bad, and stops it being picked up again. */
    private void tagBait() {
        for (ItemEntity e : world.getEntitiesOfClass(ItemEntity.class, box(), ItemEntity::isAlive)) {
            if (e.getTags().contains(GOOD_BAIT) || e.getTags().contains(BAD_BAIT)) {
                continue;
            }
            int bait = BaRules.BAIT.indexOf(Inv.idOf(e.getItem()));
            if (bait >= 0) {
                e.addTag(bait == callValue[Call.BAIT.ordinal()] ? GOOD_BAIT : BAD_BAIT);
                e.setNeverPickUp();
            }
        }
    }

    private void runners() {
        int lure = BaRules.lureRange(coveringLevel(BaRules.DEFENDER));
        BlockPos cave = rel(LURE_CAVE[0], 1, LURE_CAVE[1]);
        for (LivingEntity e : living("penance_runner")) {
            if (!(e instanceof Mob m) || stunned.containsKey(m.getUUID())) {
                continue;
            }
            Runner r = runnerAi.computeIfAbsent(m.getUUID(), k -> new Runner());
            if (m.blockPosition().distSqr(cave) <= 2 * 2 + 1) {
                escape(m);
                continue;
            }
            if (r.bait != null && !r.bait.isAlive()) {
                r.bait = null;
            }
            if (r.bait != null && m.distanceToSqr(r.bait) < 1.5 * 1.5) {
                eat(m, r);
                continue;
            }
            if (ticks < r.nextCycle) {
                continue;
            }
            r.nextCycle = ticks + BaRules.RUNNER_CYCLE_TICKS;
            r.hunting = !r.hunting;
            if (r.bait != null) {
                m.getNavigation().moveTo(r.bait.getX(), r.bait.getY(), r.bait.getZ(), 1.0);
            } else if (r.hunting && !r.confused) {
                ItemEntity bait = baitInSight(m, lure);
                if (bait != null) {
                    r.bait = bait;
                    m.getNavigation().moveTo(bait.getX(), bait.getY(), bait.getZ(), 1.0);
                }
            } else if (!r.hunting) {
                r.confused = false;
                BlockPos at = m.blockPosition().subtract(origin);
                int[] d = BaRules.runnerWalk(random.nextDouble(), at.getZ() >= halfZ() - 2, LURE_CAVE[0] - at.getX());
                if (at.getZ() >= halfZ() - 2 && d[0] == 0) {
                    d = new int[]{0, 1};
                }
                BlockPos to = clampFloor(at.getX() + d[0], at.getZ() + d[1]);
                m.getNavigation().moveTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5, 1.0);
            }
        }
    }

    private BlockPos clampFloor(int x, int z) {
        return rel(Math.max(-halfX() + 1, Math.min(halfX() - 1, x)), 1, Math.max(-halfZ() + 1, Math.min(halfZ() - 1, z)));
    }

    /** The most recently dropped bait within the runner's lure range, or null. */
    private ItemEntity baitInSight(Mob runner, int range) {
        ItemEntity best = null;
        for (ItemEntity e : world.getEntitiesOfClass(ItemEntity.class, runner.getBoundingBox().inflate(range), ItemEntity::isAlive)) {
            if ((e.getTags().contains(GOOD_BAIT) || e.getTags().contains(BAD_BAIT)) && e.distanceToSqr(runner) <= range * range
                    && (best == null || e.getAge() < best.getAge())) {
                best = e;
            }
        }
        return best;
    }

    /** A runner eating bait: good bait by a working trap kills it, good bait elsewhere just feeds it, bad bait sends it north. */
    private void eat(Mob m, Runner r) {
        ItemEntity bait = r.bait;
        r.bait = null;
        boolean good = bait.getTags().contains(GOOD_BAIT);
        ServerPlayer dropper = bait.getOwner() instanceof ServerPlayer sp ? sp : null;
        bait.discard();
        if (good) {
            int trap = trapNear(m.blockPosition());
            if (trap >= 0 && trapCharges[trap] > 0) {
                trapCharges[trap]--;
                say(m, "Urghhh!");
                sessionHit(dropper, m, (int) Math.ceil(m.getHealth()) + 5);
                if (trapCharges[trap] == 0) {
                    broadcast("The runner trap breaks! Repair it with a hammer and logs.");
                }
                return;
            }
            say(m, "Chomp, chomp.");
            return;
        }
        say(m, "Blughhhh.");
        r.confused = true;
        r.nextCycle = ticks + BaRules.RUNNER_CYCLE_TICKS;
        BlockPos at = m.blockPosition().subtract(origin);
        BlockPos to = clampFloor(at.getX(), at.getZ() + (at.getZ() < -halfZ() + 6 ? BaRules.RUNNER_STEP : -BaRules.RUNNER_STEP));
        m.getNavigation().moveTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5, 1.2);
    }

    /** The trap a runner standing here eats bait at (on or next to it), or -1. */
    int trapNear(BlockPos pos) {
        for (int i = 0; i < TRAPS.length; i++) {
            if (pos.distSqr(rel(TRAPS[i][0], 1, TRAPS[i][1])) <= 2 * 2 + 1) {
                return i;
            }
        }
        return -1;
    }

    int trapCharges(int trap) {
        return trapCharges[trap];
    }

    private void say(Mob m, String line) {
        for (ServerPlayer p : online()) {
            if (p.distanceToSqr(m) < 16 * 16) {
                Msg.action(p, "Penance Runner: " + line);
            }
        }
    }

    /** A runner reaching the lure cave escapes (a penalty) and comes back out of the northern cave. */
    private void escape(Mob m) {
        runnerAi.remove(m.getUUID());
        foodPoison.remove(m.getUUID());
        eggPoison.remove(m.getUUID());
        m.discard();
        if (wave < BaWaves.WAVES) {
            escapes++;
        }
        pending[BaWaves.RUNNER]++;
        broadcastRed("A Penance runner has escaped into the lure cave!");
    }

    private void repairTrap(ServerPlayer p, int trap) {
        if (!covers(p, BaRules.DEFENDER)) {
            Msg.game(p, "Only the defender can repair the runner traps.");
            return;
        }
        if (trapCharges[trap] >= BaRules.TRAP_CHARGES) {
            Msg.game(p, "The trap is in perfect working order.");
            return;
        }
        if (!Inv.has(p, "hammer", 1) && !Inv.has(p, "imcando_hammer", 1)) {
            Msg.game(p, "You need a hammer to repair the trap.");
            return;
        }
        if (!Inv.has(p, "logs", 1)) {
            Msg.game(p, "You need logs to repair the trap.");
            return;
        }
        useLentFirst(p, "logs");
        trapCharges[trap] = BaRules.TRAP_CHARGES;
        Msg.game(p, "You repair the runner trap.");
    }

    // ------------------------------------------------------------------ healer

    private void healers() {
        List<LivingEntity> runnerMobs = living("penance_runner");
        for (LivingEntity e : living("penance_healer")) {
            if (!(e instanceof Mob m) || stunned.containsKey(m.getUUID())) {
                continue;
            }
            Healer h = healerAi.computeIfAbsent(m.getUUID(), k -> new Healer());
            switch (h.mode) {
                case HEALER_CHASE_PLAYER -> {
                    ServerPlayer target = nearestPlayer(m);
                    if (target == null) {
                        h.mode = HEALER_WANDER;
                        h.until = ticks + 10;
                    } else if (m.distanceToSqr(target) < 1.8 * 1.8) {
                        PoisonService.poison(target, BaWaves.HEALER_POISON, ActionManager.gameTick());
                        h.mode = HEALER_WANDER;
                        h.until = ticks + 10; // approx: "wander for a short duration"
                    } else if (ticks >= h.nextPath) {
                        h.nextPath = ticks + 3;
                        m.getNavigation().moveTo(target, 1.0);
                    }
                }
                case HEALER_WANDER -> {
                    if (ticks >= h.until) {
                        // With no runners left (none alive or to come), a healer just wanders for the rest of the wave.
                        h.mode = !runnerMobs.isEmpty() ? HEALER_CHASE_RUNNER
                                : pending[BaWaves.RUNNER] + reserve[BaWaves.RUNNER] > 0 ? HEALER_CHASE_PLAYER : HEALER_WANDER;
                        h.until = ticks + 10;
                    }
                }
                default -> {
                    LivingEntity runner = nearest(runnerMobs, m.blockPosition());
                    if (runner == null) {
                        h.mode = HEALER_WANDER;
                        h.until = ticks + 10;
                    } else if (m.distanceToSqr(runner) < 1.8 * 1.8) {
                        runner.setHealth(runner.getMaxHealth());
                        eggPoison.remove(runner.getUUID()); // a green egg's poison hits five times "unless healed"
                        world.sendParticles(ParticleTypes.HEART, runner.getX(), runner.getY() + 1, runner.getZ(), 3, 0.3, 0.3, 0.3, 0);
                        h.mode = HEALER_CHASE_PLAYER;
                    } else if (ticks >= h.nextPath) {
                        h.nextPath = ticks + 3;
                        m.getNavigation().moveTo(runner, 1.0);
                    }
                }
            }
        }
    }

    private ServerPlayer nearestPlayer(Mob m) {
        ServerPlayer best = null;
        for (ServerPlayer p : online()) {
            if (best == null || p.distanceToSqr(m) < best.distanceToSqr(m)) {
                best = p;
            }
        }
        return best;
    }

    /**
     * Poisoned food used on a Penance healer: the called food hits for 4 and poisons (4, dropping by 1 every 5 hits; a new food
     * resets it); the wrong food does nothing and costs points.
     */
    boolean useFood(ServerPlayer p, LivingEntity healer, String food) {
        int option = BaRules.POISON_FOOD.indexOf(food);
        if (option < 0 || typeOf(MinigameManager.creatureId(healer)) != BaWaves.HEALER) {
            return false;
        }
        if (!covers(p, BaRules.HEALER)) {
            Msg.game(p, "Only the healer can use poisoned food.");
            return true;
        }
        Inv.remove(p, food, 1);
        if (option != callValue[Call.POISON.ordinal()]) {
            if (wave < BaWaves.WAVES) {
                wrongPoison++;
            }
            p.displayClientMessage(Component.literal("That's the wrong type of poisoned food to use! Penalty!").withStyle(ChatFormatting.RED),
                    false);
            return true;
        }
        Poison ps = new Poison();
        ps.by = p.getUUID();
        ps.interval = BaRules.POISON_INTERVAL_TICKS;
        ps.next = ticks + ps.interval;
        foodPoison.put(healer.getUUID(), ps);
        sessionHit(p, healer, BaRules.FOOD_DAMAGE);
        return true;
    }

    /** Both kinds of poison tick on their own: the food's (4 dropping to 1) and the green egg's (1, five times). */
    private void poison() {
        tickPoison(foodPoison, true);
        tickPoison(eggPoison, false);
    }

    private void tickPoison(Map<UUID, Poison> poisons, boolean food) {
        List<UUID> done = new ArrayList<>();
        // A copy: a poison hit that kills its creature removes it from the map (onKill).
        for (Map.Entry<UUID, Poison> e : new ArrayList<>(poisons.entrySet())) {
            Poison ps = e.getValue();
            if (ticks < ps.next || stunned.containsKey(e.getKey())) {
                continue;
            }
            LivingEntity mob = null;
            for (LivingEntity l : living(null)) {
                if (l.getUUID().equals(e.getKey())) {
                    mob = l;
                }
            }
            int hit = food ? BaRules.foodPoisonHit(ps.hits) : BaRules.greenPoisonHit(ps.hits);
            if (mob == null || hit <= 0) {
                done.add(e.getKey());
                continue;
            }
            ps.hits++;
            ps.next = ticks + ps.interval;
            sessionHit(world.getServer().getPlayerList().getPlayer(ps.by), mob, hit);
        }
        done.forEach(poisons::remove);
    }

    /** The healing vial: filled at the spring, a dose heals a teammate by the healer's level and cures poison. */
    boolean useVial(ServerPlayer healer, ServerPlayer target, String vial) {
        if (!vial.startsWith(BaRules.HEALING_VIAL + "_") || !has(target)) {
            return false;
        }
        if (!covers(healer, BaRules.HEALER)) {
            Msg.game(healer, "Only the healer can use the healing vial.");
            return true;
        }
        int doses = Integer.parseInt(vial.substring(BaRules.HEALING_VIAL.length() + 1));
        float before = target.getHealth();
        target.heal(BaRules.vialHeal(roleLevel(healer, BaRules.HEALER)));
        PoisonService.cure(target, false, 0, ActionManager.gameTick());
        int healed = Math.round(target.getHealth() - before);
        if (target != healer && wave < BaWaves.WAVES) {
            hitpointsHealed += healed;
        }
        Inv.remove(healer, vial, 1);
        Inv.give(healer, doses > 1 ? BaRules.HEALING_VIAL + "_" + (doses - 1) : BaRules.HEALING_VIAL, 1);
        Msg.game(healer, "You heal " + (target == healer ? "yourself" : target.getName().getString()) + " for " + healed + ".");
        return true;
    }

    private void spring(ServerPlayer p) {
        if (Inv.has(p, BaRules.HEALING_VIAL, 1)) {
            Inv.remove(p, BaRules.HEALING_VIAL, 1);
            Inv.give(p, BaRules.HEALING_VIAL + "_" + BaRules.VIAL_DOSES, 1);
            Msg.game(p, "You fill the healing vial at the spring.");
            return;
        }
        if (!covers(p, BaRules.HEALER)) {
            Msg.game(p, "Only healers can drink from the spring.");
            return;
        }
        int heal = BaRules.springSelfHeal(roleLevel(p, BaRules.HEALER), Math.round(p.getMaxHealth()));
        p.heal(heal);
        PoisonService.cure(p, false, 0, ActionManager.gameTick());
        Msg.game(p, "You drink from the spring.");
    }

    // ------------------------------------------------------------------ wave 10: the Penance Queen

    private void queenWave() {
        if (queen == null) {
            if (queenEmergeAt < 0) {
                if (ticks - waveStart >= BaWaves.QUEEN_TIMEOUT_TICKS) {
                    queenEmergeAt = ticks;
                } else if (ticks >= queenCheckAt) {
                    queenCheckAt = ticks + BaWaves.QUEEN_CHECK_TICKS;
                    if (allOut() && penanceAlive() == 0) {
                        queenEmergeAt = ticks + BaWaves.QUEEN_EMERGE_TICKS;
                    }
                }
            }
            if (queenEmergeAt >= 0 && ticks >= queenEmergeAt) {
                emergeQueen();
            }
            return;
        }
        if (!queen.isAlive()) {
            return;
        }
        if (ticks >= nextVolley) {
            nextVolley = ticks + BaWaves.QUEEN_VOLLEY_TICKS;
            // Her unblockable volley hits every player, wherever they stand.
            for (ServerPlayer p : online()) {
                world.sendParticles(ParticleTypes.ITEM_SLIME, p.getX(), p.getY() + 1, p.getZ(), 6, 0.3, 0.3, 0.3, 0);
                hurt(p, random.nextInt(BaWaves.QUEEN.maxHit() + 1));
            }
        }
        if (ticks >= nextQueenSpawn) {
            nextQueenSpawn = ticks + BaWaves.QUEEN_SPAWN_TICKS;
            if (living("queen_spawn").size() < BaWaves.QUEEN_SPAWN_MAX) {
                Mob spawn = spawn("queen_spawn", randomFloor(QUEEN_TRAPDOOR[0], QUEEN_TRAPDOOR[1] + 3, 3));
                if (spawn != null) {
                    setHitpoints(spawn, BaWaves.QUEEN_SPAWN.hitpoints());
                    aggro(spawn);
                }
            }
        }
        if (ticks >= nextYellowEgg) {
            nextYellowEgg = ticks + BaWaves.YELLOW_EGG_TICKS;
            dropEgg(BaRules.YELLOW_EGG, 1, queen.position().add(-2.5, 0.2, 2.5));
        }
    }

    void emergeQueen() {
        queen = spawn("penance_queen", rel(QUEEN_TRAPDOOR[0], 1, QUEEN_TRAPDOOR[1]));
        if (queen == null) {
            return;
        }
        setHitpoints(queen, BaWaves.QUEEN.hitpoints());
        aggro(queen);
        phase = Phase.QUEEN;
        nextVolley = ticks + BaWaves.QUEEN_VOLLEY_TICKS;
        nextQueenSpawn = ticks + BaWaves.QUEEN_SPAWN_TICKS;
        nextYellowEgg = ticks + BaWaves.YELLOW_EGG_TICKS;
        broadcastRed("The Penance Queen emerges from the trapdoor! Make omega eggs: collector -> healer -> attacker -> defender -> "
                + "collector.");
    }

    Mob queen() {
        return queen;
    }

    /** A step of the omega egg at a crater or the mushroom: the role's egg comes out one step further. */
    private void omegaStep(ServerPlayer p, BaRules.OmegaStep step) {
        if (!covers(p, step.role())) {
            Msg.game(p, "That's the " + BarbarianAssault.ROLES[step.role()] + "'s job.");
            return;
        }
        int n = Inv.remove(p, step.egg(), 10_000);
        if (n == 0) {
            Msg.game(p, "You need " + com.runecraft.osrs.ItemCatalog.get(step.egg()).name().toLowerCase(Locale.ROOT) + "s for this.");
            return;
        }
        Inv.give(p, step.result(), n);
        Msg.game(p, "You make " + n + " " + com.runecraft.osrs.ItemCatalog.get(step.result()).name().toLowerCase(Locale.ROOT)
                + (n == 1 ? "" : "s") + ". Pass " + (n == 1 ? "it" : "them") + " to the "
                + BarbarianAssault.ROLES[BaRules.handTo(step.result())] + ".");
    }

    /** Using an egg on a teammate hands it over (the omega egg is passed round the team). */
    boolean handEgg(ServerPlayer from, ServerPlayer to, String egg) {
        if (!has(to) || to == from || !(WAVE_ITEMS.contains(egg) && egg.endsWith("_egg"))) {
            return false;
        }
        Inv.remove(from, egg, 1);
        Inv.give(to, egg, 1);
        Msg.game(from, "You hand the egg to " + to.getName().getString() + ".");
        Msg.game(to, from.getName().getString() + " hands you an egg.");
        return true;
    }

    // ------------------------------------------------------------------ arena objects

    @Override
    protected boolean onBlock(ServerPlayer player, String blockId, BlockPos pos) {
        switch (blockId) {
            case "ba_egg_launcher" -> launcher(player, launcherAt(pos));
            case "ba_healer_spring" -> spring(player);
            case "ba_attacker_machine" -> attackerMachine(player);
            case "ba_defender_machine" -> {
                if (covers(player, BaRules.DEFENDER)) {
                    topUp(player, BaRules.BAIT, 5);
                    Msg.game(player, "You take bait from the defender item machine. Drop the called bait (ear) to lure runners.");
                } else {
                    Msg.game(player, "Only the defender can use this machine.");
                }
            }
            case "ba_healer_machine" -> {
                if (covers(player, BaRules.HEALER)) {
                    boolean anyVial = Inv.has(player, BaRules.HEALING_VIAL, 1);
                    for (int d = 1; d <= BaRules.VIAL_DOSES; d++) {
                        anyVial |= Inv.has(player, BaRules.HEALING_VIAL + "_" + d, 1);
                    }
                    if (!anyVial) {
                        Inv.give(player, BaRules.HEALING_VIAL, 1);
                    }
                    topUp(player, BaRules.POISON_FOOD, 5);
                    Msg.game(player, "You take poisoned food and a healing vial from the healer item machine.");
                } else {
                    Msg.game(player, "Only the healer can use this machine.");
                }
            }
            case "ba_collector_converter" -> {
                if (covers(player, BaRules.COLLECTOR)) {
                    convert(player);
                } else {
                    Msg.game(player, "Only the collector can use the converter.");
                }
            }
            case "ba_runner_trap" -> repairTrap(player, trapAt(pos));
            case "ba_log_pile" -> {
                if (phase == Phase.WAITING || ticks < logsBackAt) {
                    Msg.game(player, "There are no logs here right now.");
                } else {
                    logsBackAt = ticks + BaRules.LOG_RESPAWN_TICKS;
                    lend(player, "logs", 1);
                    Msg.game(player, "You take some logs.");
                }
            }
            case "ba_hammer" -> {
                if (Inv.has(player, "hammer", 1)) {
                    Msg.game(player, "You already have a hammer.");
                } else {
                    lend(player, "hammer", 1);
                    Msg.game(player, "You pick up the hammer.");
                }
            }
            case "ba_ladder" -> ladder(player);
            case "ba_lure_cave" -> Msg.game(player, "The runners escape through here. Don't let them!");
            default -> {
                BaRules.OmegaStep step = BaRules.stepAt(blockId);
                if (step == null) {
                    return false;
                }
                omegaStep(player, step);
            }
        }
        return true;
    }

    private int trapAt(BlockPos pos) {
        BlockPos r = pos.subtract(origin);
        return r.getX() == TRAPS[1][0] && r.getZ() == TRAPS[1][1] ? 1 : 0;
    }

    /** The attacker item machine: the four arrows (sneak for runes: the four elements and the autocast spell's catalytic rune). */
    private void attackerMachine(ServerPlayer p) {
        if (!covers(p, BaRules.ATTACKER)) {
            Msg.game(p, "Only the attacker can use this machine.");
            return;
        }
        if (!p.isShiftKeyDown()) {
            for (String arrow : BaRules.ARROWS) {
                int have = Inv.count(p, arrow);
                if (have < 64) {
                    Inv.give(p, arrow, 64 - have);
                }
            }
            Msg.game(p, "You take arrows: bullet (controlled), field (accurate), blunt (aggressive) and barbed (defensive).");
            return;
        }
        List<Rune> runes = new ArrayList<>(List.of(Rune.AIR, Rune.WATER, Rune.EARTH, Rune.FIRE));
        Spell spell = Players.data(p).autocast();
        if (spell != null) {
            for (Rune r : spell.runes().keySet()) {
                if (!runes.contains(r)) {
                    runes.add(r);
                }
            }
        } else {
            runes.add(Rune.MIND);
        }
        for (Rune r : runes) {
            // The arena's own runes (OSRS's untradeable variants): the player's own runes aren't counted or used first.
            int have = lentCount(p, r.itemId());
            if (have < 64) {
                lend(p, r.itemId(), 64 - have); // approx: OSRS hands out 1,000 of each, more than a Minecraft stack
            }
        }
        Msg.game(p, "You take runes for elemental spells: wind, water, earth and fire answer the four style calls.");
    }

    private void topUp(ServerPlayer p, List<String> items, int each) {
        for (String item : items) {
            int want = each - Inv.count(p, item);
            for (int i = 0; i < want && p.getInventory().getFreeSlot() >= 0; i++) {
                Inv.give(p, item, 1);
            }
        }
    }

    /** The waiting room's ladder: the leader starts the next wave at once instead of waiting a minute. */
    private void ladder(ServerPlayer p) {
        if (phase != Phase.WAITING) {
            Msg.game(p, "The wave is already under way.");
            return;
        }
        if (first() != p) {
            Msg.game(p, "Only the team leader can start the wave early.");
            return;
        }
        nextWaveIn = 1;
        Msg.game(p, "You start wave " + wave + ".");
    }

    // ------------------------------------------------------------------ HUD

    @Override
    protected List<String> hud(ServerPlayer player) {
        int r = role(player);
        int honour = MinigameData.of(player).points(player.getUUID(), "ba_" + BarbarianAssault.ROLES[r]);
        List<String> lines = new ArrayList<>();
        lines.add("Barbarian Assault - wave " + wave + " / " + BaWaves.WAVES
                + (phase == Phase.WAITING ? " (starts in " + Math.max(0, nextWaveIn * 6 / 10) + "s)" : ""));
        lines.add("Role: " + roleName(r) + " lv " + roleLevel(player, r) + " (" + honour + " honour)");
        // The calls hold until every Penance is dead: on wave 10 the queen can come out (after 4 minutes) with some still alive,
        // and the frozen calls still decide their style, eggs and food (calls stop changing and the horn falls silent then).
        if (phase == Phase.RUNNING || phase == Phase.QUEEN && (penanceAlive() > 0 || !allOut())) {
            Call make = Call.madeBy(r);
            int value = callValue[make.ordinal()];
            lines.add("Call (" + roleName(make.receiver()) + "): " + make.label(value) + (called[make.ordinal()] == value ? " - called" : ""));
            Call heard = Call.heardBy(r);
            lines.add("Do: " + heard.label(told(player)));
            for (Call c : Call.values()) {
                // Calls of roles nobody plays come to everyone covering them.
                if (c != heard && !teamHas(c.receiver()) && c.receiver() != r) {
                    lines.add("Do (" + BarbarianAssault.ROLES[c.receiver()] + "): " + c.label(callValue[c.ordinal()]));
                }
            }
            lines.add("Penance: " + living("penance_fighter").size() + " F, " + living("penance_ranger").size() + " R, "
                    + living("penance_runner").size() + " Ru, " + living("penance_healer").size() + " H");
        }
        if (phase != Phase.WAITING) {
            if (covers(player, BaRules.COLLECTOR)) {
                int[] bag = bag(player);
                lines.add("Bag: " + (bag[0] + bag[1] + bag[2]) + "/" + bagCapacity() + "  Hopper: " + hopperText());
            }
            if (covers(player, BaRules.DEFENDER)) {
                lines.add("Traps: east " + trapCharges[0] + ", west " + trapCharges[1]);
            }
        }
        if (queen != null && queen.isAlive()) {
            lines.add("Queen: " + (int) queen.getHealth() + " / " + BaWaves.QUEEN.hitpoints());
        }
        return lines;
    }

    // ------------------------------------------------------------------ hooks (registered once by MinigamesHooks)

    private static BarbarianAssaultSession of(ServerPlayer p) {
        return MinigameManager.session(p) instanceof BarbarianAssaultSession s && !s.isEnded() ? s : null;
    }

    /** Combat, item and talk hooks, and the /ba command (horn calls, the egg launcher's menu, Captain Cain's reset). */
    public static void register() {
        CombatHooks.addMonsterDamageGate((player, target) ->
                MinigameManager.owner(target) instanceof BarbarianAssaultSession s && !s.isEnded() ? s.deny(player, target) : null);
        CombatHooks.addModifier(new CombatHooks.PlayerAttackModifier() {
            @Override
            public int afterRoll(ServerPlayer player, LivingEntity target, AttackType type, int damage) {
                BarbarianAssaultSession s = of(player);
                return s != null && MinigameManager.owner(target) == s ? s.attackRoll(player, target, type, damage) : damage;
            }
        });
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof AbstractArrow arrow && arrow.getOwner() instanceof ServerPlayer p && of(p) != null) {
                for (String tag : arrow.getTags()) {
                    if (tag.startsWith("runecraft_arrow_")) {
                        LAST_ARROW.put(p.getUUID(), tag.substring("runecraft_arrow_".length()));
                    }
                }
            }
        });
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClientSide() || hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer sp)) {
                return InteractionResult.PASS;
            }
            if (entity instanceof com.runecraft.entity.QuestNpcEntity npc && npc.npcId().equals("mg_commander_connad")
                    && sellBack(sp, Inv.idOf(sp.getMainHandItem()))) {
                return InteractionResult.SUCCESS;
            }
            BarbarianAssaultSession s = of(sp);
            if (s == null) {
                return InteractionResult.PASS;
            }
            String held = Inv.idOf(sp.getMainHandItem());
            boolean used = false;
            if (entity instanceof ServerPlayer target) {
                used = s.useVial(sp, target, held) || s.handEgg(sp, target, held);
            } else if (entity instanceof LivingEntity living && MinigameManager.owner(living) == s) {
                used = s.useFood(sp, living, held);
            }
            return used ? InteractionResult.SUCCESS : InteractionResult.PASS;
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (world.isClientSide() || !(player instanceof ServerPlayer sp) || stack.isEmpty()) {
                return InteractionResultHolder.pass(stack);
            }
            String id = Inv.idOf(stack);
            BarbarianAssaultSession s = of(sp);
            if (s != null && id.endsWith("_horn") && KEPT_ITEMS.contains(id)) {
                s.hornMenu(sp);
                return InteractionResultHolder.success(stack);
            }
            if (s != null && id.equals("collection_bag")) {
                int[] bag = s.bag(sp);
                Msg.game(sp, "Your collection bag holds " + bag[0] + " red, " + bag[1] + " green and " + bag[2] + " blue eggs (room for "
                        + s.bagCapacity() + ").");
                return InteractionResultHolder.success(stack);
            }
            if (s != null && id.startsWith(BaRules.HEALING_VIAL + "_")) {
                s.useVial(sp, sp, id);
                return InteractionResultHolder.success(stack);
            }
            return InteractionResultHolder.pass(stack);
        });
        // The arena's lent runes, logs and hammer vanish outside it (OSRS's untradeable variants): a player logging in outside a
        // game (after a crash or a restart mid-wave) loses any still in the pack.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer p = handler.getPlayer();
            if (of(p) == null && stripLent(p) > 0) {
                Msg.game(p, "The runes and tools you took from Barbarian Assault crumble away.");
            }
        });
        QuestScripts.onTalk("mg_captain_cain", (player, npc) -> captainCain(player));
        java.util.function.BiFunction<ServerPlayer, com.runecraft.osrs.Prayer, String> otherRules = com.runecraft.combat.PrayerService.minigameRule;
        com.runecraft.combat.PrayerService.minigameRule = (player, prayer) -> of(player) != null
                ? "You can't use prayers in Barbarian Assault." : otherRules.apply(player, prayer);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                Commands.literal("ba")
                        .then(Commands.literal("call").then(Commands.argument("word", StringArgumentType.word()).executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayer();
                            BarbarianAssaultSession s = p == null ? null : of(p);
                            if (s != null) {
                                String word = StringArgumentType.getString(ctx, "word");
                                if (word.equals("medic")) {
                                    s.medic(p);
                                } else {
                                    s.call(p, word);
                                }
                            }
                            return 1;
                        })))
                        .then(Commands.literal("fire").then(Commands.argument("colour", StringArgumentType.word())
                                .then(Commands.argument("target", StringArgumentType.word()).executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    BarbarianAssaultSession s = p == null ? null : of(p);
                                    if (s != null) {
                                        s.fire(p, StringArgumentType.getString(ctx, "colour"), StringArgumentType.getString(ctx, "target"));
                                    }
                                    return 1;
                                }))))
                        .then(Commands.literal("resetwave").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayer();
                            if (p != null) {
                                resetWave(p);
                            }
                            return 1;
                        }))));
    }

    /**
     * Penance gear used on Commander Connad: he takes it back for 80% of its honour points in each role (OSRS Wiki); queen kills
     * and coins (the granite body) aren't refunded. Returns false for anything he doesn't sell for honour points.
     */
    static boolean sellBack(ServerPlayer p, String item) {
        com.runecraft.osrs.content.minigames.RewardShop shop = com.runecraft.osrs.content.minigames.RewardShops.get("barbarian_assault");
        if (shop == null || item.isEmpty()) {
            return false;
        }
        String[] currencies = {com.runecraft.osrs.content.minigames.MinigameCurrencies.BA_ATTACKER,
                com.runecraft.osrs.content.minigames.MinigameCurrencies.BA_DEFENDER,
                com.runecraft.osrs.content.minigames.MinigameCurrencies.BA_COLLECTOR,
                com.runecraft.osrs.content.minigames.MinigameCurrencies.BA_HEALER};
        for (com.runecraft.osrs.content.minigames.RewardShop.Entry e : shop.entries()) {
            if (!e.item().equals(item)) {
                continue;
            }
            int[] refund = new int[currencies.length];
            boolean honour = false;
            for (com.runecraft.osrs.content.minigames.RewardShop.Cost c : e.costs()) {
                for (int r = 0; r < currencies.length; r++) {
                    if (c.currency().equals(currencies[r])) {
                        refund[r] = BaRules.refund(c.amount());
                        honour = true;
                    }
                }
            }
            if (!honour || Inv.remove(p, item, 1) == 0) {
                return false;
            }
            MinigameData data = MinigameData.of(p);
            for (int r = 0; r < refund.length; r++) {
                String key = "ba_" + BarbarianAssault.ROLES[r];
                data.addPoints(p.getUUID(), key, BarbarianAssault.addHonour(data.points(p.getUUID(), key), refund[r]));
            }
            QuestApi.say(p, "Commander Connad", "I'll take that back. Here are " + refund[0] + " honour points in each role.");
            return true;
        }
        return false;
    }

    /** Captain Cain: the tutorial in short, and resetting the player's round to wave 1. */
    static void captainCain(ServerPlayer p) {
        String cain = "Captain Cain";
        QuestApi.say(p, cain, "Welcome to Barbarian Assault! Pick your role at one of the four doors: attacker, defender, collector "
                + "or healer. A team of up to five fights ten waves of Penance and then their Queen.");
        QuestApi.say(p, cain, "Attackers fight the fighters and rangers with the style the collector calls; collectors pick up the eggs "
                + "the attacker calls and load the egg launcher; defenders lure runners onto the traps with the bait the healer calls; "
                + "healers poison the Penance healers with the food the defender calls and heal the team.");
        QuestApi.say(p, cain, "Use your horn to call every 30 seconds. On wave 10 only omega eggs hurt the Queen: collector, healer, "
                + "attacker, defender, then back to the collector to load. Nobody in your role? Then that job is yours too.");
        int reached = Math.max(1, waveReached(p));
        p.displayClientMessage(Component.literal("You have reached wave " + reached + " this round. ").withStyle(ChatFormatting.GRAY)
                .append(link("[Reset my round to wave 1]", "/ba resetwave")), false);
    }

    /** Resets a player's round to wave 1 (Captain Cain, or the wave 1 barrier in OSRS). */
    static void resetWave(ServerPlayer p) {
        if (MinigameManager.session(p) != null) {
            Msg.game(p, "You can't do that during a game.");
            return;
        }
        MinigameData.of(p).setStat(p.getUUID(), WAVE_STAT, 0);
        QuestApi.say(p, "Captain Cain", "Very well, your next game starts from wave 1.");
    }

    // ------------------------------------------------------------------ role levels, gambles and the blackboard

    /** A player's level (1-5) in a role, bought with that role's honour points. */
    public static int roleLevel(ServerPlayer p, int role) {
        return Math.max(1, MinigameData.of(p).stat(p.getUUID(), "ba_level_" + BarbarianAssault.ROLES[role]));
    }

    /** Stat key: the wave a player has reached in the current round (0 after the queen dies). */
    public static final String WAVE_STAT = "ba_wave_reached";

    /** The wave a player has reached in the current round (gates low and medium gambles). */
    public static int waveReached(ServerPlayer p) {
        return MinigameData.of(p).stat(p.getUUID(), WAVE_STAT);
    }

    /** Runs one gamble whose cost the reward shop has already taken. */
    public static void gamble(ServerPlayer p, com.runecraft.osrs.content.minigames.BaGambles.Tier tier) {
        if (tier == null) {
            return;
        }
        java.util.Random rng = new java.util.Random(p.getRandom().nextLong());
        for (com.runecraft.osrs.content.minigames.BaGambles.Prize prize : com.runecraft.osrs.content.minigames.BaGambles.roll(tier,
                rng::nextDouble, com.runecraft.osrs.ItemCatalog::exists)) {
            if (prize.noted()) {
                com.runecraft.content.minigames.MinigameItemUse.giveNoted(p, prize.item(), prize.amount());
            } else {
                reward(p, prize.item(), prize.amount());
            }
            if (prize.item().equals("pet_penance_queen")) {
                p.getServer().getPlayerList().broadcastSystemMessage(Component.literal(p.getName().getString()
                        + " has a funny feeling like they're being followed.").withStyle(ChatFormatting.GOLD), false);
            }
        }
        MinigameData.of(p).incrementStat(p.getUUID(), "ba_gambles_" + tier.name().toLowerCase(Locale.ROOT));
        Msg.game(p, "Commander Connad hands you the prize of your " + tier.displayName().toLowerCase(Locale.ROOT) + ".");
    }

    /** Every role's level. */
    public static int[] roleLevels(ServerPlayer p) {
        int[] levels = new int[BarbarianAssault.ROLES.length];
        for (int r = 0; r < levels.length; r++) {
            levels[r] = roleLevel(p, r);
        }
        return levels;
    }

    /** Buys the next level in a role with its honour points; returns whether it did. */
    public static boolean buyLevel(ServerPlayer p, int role) {
        int level = roleLevel(p, role);
        String name = roleName(role);
        if (level >= BarbarianAssault.MAX_ROLE_LEVEL) {
            Msg.game(p, "You are already level " + BarbarianAssault.MAX_ROLE_LEVEL + " as a " + name + ".");
            return false;
        }
        int cost = BarbarianAssault.levelCost(level + 1);
        String key = "ba_" + BarbarianAssault.ROLES[role];
        MinigameData data = MinigameData.of(p);
        if (data.points(p.getUUID(), key) < cost) {
            Msg.game(p, "You need " + cost + " " + name + " honour points for level " + (level + 1) + ".");
            return false;
        }
        data.addPoints(p.getUUID(), key, -cost);
        data.setStat(p.getUUID(), "ba_level_" + BarbarianAssault.ROLES[role], level + 1);
        Msg.game(p, "You are now a level " + (level + 1) + " " + name + ".");
        return true;
    }

    /** Buys a level by role name ("attacker", "defender", "collector" or "healer"). */
    public static void buyLevel(ServerPlayer p, String roleName) {
        for (int r = 0; r < BarbarianAssault.ROLES.length; r++) {
            if (BarbarianAssault.ROLES[r].equals(roleName)) {
                buyLevel(p, r);
                return;
            }
        }
        Msg.game(p, "There is no such role.");
    }

    /** The blackboard: honour, level and next upgrade per role, with links to buy upgrades. */
    public static void readBlackboard(ServerPlayer p) {
        MinigameData data = MinigameData.of(p);
        p.displayClientMessage(Component.literal("Barbarian Assault - honour points and role levels").withStyle(ChatFormatting.GOLD), false);
        for (int r = 0; r < BarbarianAssault.ROLES.length; r++) {
            int level = roleLevel(p, r);
            int honour = data.points(p.getUUID(), "ba_" + BarbarianAssault.ROLES[r]);
            MutableComponent line = Component.literal(" " + roleName(r) + ": level " + level + ", " + honour + " honour")
                    .withStyle(ChatFormatting.GRAY);
            if (level < BarbarianAssault.MAX_ROLE_LEVEL) {
                String command = "/minigame balevel " + BarbarianAssault.ROLES[r];
                line.append(Component.literal(" [Upgrade: " + BarbarianAssault.levelCost(level + 1) + "]").withStyle(st -> st
                        .withColor(ChatFormatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))));
            }
            p.displayClientMessage(line, false);
        }
        p.displayClientMessage(Component.literal(" Wave reached this round: " + Math.max(1, waveReached(p)) + "; queen kills: "
                + data.points(p.getUUID(), "ba_queen_kills")).withStyle(ChatFormatting.GRAY), false);
        GameEvents.fireActivity(p, "ba_blackboard");
        if (BarbarianAssault.allMaxed(roleLevels(p))) {
            GameEvents.fireActivity(p, "ba_blackboard:level5");
        }
    }

    // ------------------------------------------------------------------ self-test access (BarbarianAssaultSelfTest)

    int wave() {
        return wave;
    }

    Phase phase() {
        return phase;
    }

    int hopper(int colour) {
        return hopper[colour];
    }

    int pending(int type) {
        return pending[type];
    }

    int reserve(int type) {
        return reserve[type];
    }

    /** Sets a call's value and stops the calls changing (the self-test picks the calls instead of rolling them). */
    void setCall(Call c, int value) {
        callValue[c.ordinal()] = value;
        nextCall = ticks + 100_000;
    }

    /** The self-test's ticks: one OSRS game tick of the session. */
    long ticks() {
        return ticks;
    }

    BlockPos at(int[] offset) {
        return rel(offset[0], 1, offset[1]);
    }

    /** Kills a Penance through the session's own damage (as an egg or a trap would), credited to the first player. */
    void selfTestKill(LivingEntity mob) {
        sessionHit(first(), mob, (int) Math.ceil(mob.getHealth()) + 5);
    }

    List<LivingEntity> penance(int type) {
        return living(BarbarianAssault.PENANCE[type]);
    }

    /** Whether a poisoned food's poison is running on a Penance creature. */
    boolean foodPoisoned(LivingEntity mob) {
        return foodPoison.containsKey(mob.getUUID());
    }

    /** The interval (ticks) of a green egg's poison on a Penance creature, or -1 when none runs. */
    int eggPoisonInterval(LivingEntity mob) {
        Poison ps = eggPoison.get(mob.getUUID());
        return ps == null ? -1 : ps.interval;
    }
}
