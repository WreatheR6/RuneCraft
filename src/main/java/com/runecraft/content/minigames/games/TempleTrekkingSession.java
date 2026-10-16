package com.runecraft.content.minigames.games;

import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.MonsterStats;
import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameReward;
import com.runecraft.osrs.content.minigames.TempleTrekking;
import com.runecraft.osrs.content.minigames.TempleTrekking.Event;
import com.runecraft.osrs.content.minigames.TempleTrekking.EventType;
import com.runecraft.osrs.content.minigames.TempleTrekking.Follower;
import com.runecraft.osrs.content.minigames.TempleTrekking.Reward;
import com.runecraft.osrs.content.minigames.TempleTrekking.Route;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestApi;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Temple Trekking and the Burgh de Rott Ramble (see {@link TempleTrekking}). The trek is a run of event areas: each event
 * pulls the player and their follower into a stretch of swamp (west end: the Escape portal; east end: the Continue-trek path;
 * on routes 1 and 2 an Evade-event path beside it). Combat events send monsters at the follower; the player has to put
 * themselves in the way (a monster the player hits turns on them). Juvinates can only be hurt with silver weapons. Puzzle
 * events need the bridge repaired with logs or planks (and an axe or hammer) or three vines cut with a knife for the swing.
 * Reaching the destination gives a reward token holding the trek's points; the follower's death or the player's fails it.
 */
public class TempleTrekkingSession extends MinigameSession {
    public static final String POINTS_NBT = "runecraft_trek_points";
    /** Weapons that hurt vampyre juvinates (silver weaponry, OSRS Wiki "Temple Trekking"). */
    private static final List<String> SILVER = List.of("rod_of_ivandis", "ivandis_flail", "blisterwood_flail", "wolfbane", "silverlight",
            "darklight", "arclight", "emberlight", "blessed_axe", "silver_sickle_b");
    private static final int CHANNEL = 2;

    private final Follower follower;
    private final Route route;
    private List<Event> events = List.of();
    private int index = -1;
    private Event event;
    private MobEntity companion;
    private int points;
    private int spawned;
    private int killed;
    private int bridged;
    private boolean solved;
    private boolean lumberjackPiece;
    private boolean arrived;

    public TempleTrekkingSession(int id, int slot, ServerWorld world, String variant) {
        super(Minigame.TEMPLE_TREKKING, id, slot, world, variant);
        String[] parts = variant.split(":");
        this.follower = parts.length == 3 ? Follower.byId(parts[1]) : null;
        Route r = null;
        try {
            r = parts.length == 3 ? Route.byNumber(Integer.parseInt(parts[2])) : null;
        } catch (NumberFormatException ignored) {
            // invalid variant: the manager's requirement check refuses it
        }
        this.route = r == null ? Route.ROUTE_1 : r;
    }

    // ------------------------------------------------------------------ choosing a follower and a route

    /** The variant string a follower and route start (checked by {@link #requirement}). */
    public static String variant(Follower f, Route r) {
        return "trek:" + f.id() + ":" + r.number();
    }

    /** First unmet requirement, or null. */
    public static String requirement(ServerPlayerEntity player, String variant) {
        String[] parts = variant.split(":");
        Follower f = parts.length == 3 ? Follower.byId(parts[1]) : null;
        if (f == null) {
            return "Talk to one of the followers to choose who to escort and which route to take.";
        }
        if (!QuestApi.isComplete(player, TempleTrekking.QUEST)) {
            return "You need to complete In Aid of the Myreque to escort travellers through Morytania.";
        }
        if (f.ramble && !QuestApi.isComplete(player, TempleTrekking.RAMBLE_QUEST)) {
            return "You need to complete Darkness of Hallowvale to take part in the Burgh de Rott Ramble.";
        }
        return null;
    }

    /** A follower's dialogue: their difficulty and the three routes as clickable options. */
    public static void talk(ServerPlayerEntity player, Follower f) {
        String where = f.ramble ? "Burgh de Rott" : "the Paterdomus temple";
        QuestApi.say(player, f.displayName, "Will you escort me to " + where + "? I'm " + switch (f.difficulty) {
            case EASY -> "handy with a weapon, so only three monsters will come for us at a time.";
            case MEDIUM -> "no great fighter: four monsters will come for us at a time.";
            case HARD -> "weak and slow: five or six monsters will come for us at a time, but the reward will be bigger.";
        });
        int cb = Players.data(player).skills().combatLevel();
        MutableText line = Text.literal("Which route? ").formatted(Formatting.GRAY);
        String[] names = {"Route 1: Mort Myre (monsters ~level 80)", "Route 2: River Salve (~level 110)", "Route 3: Canifis and the Hollows (~level 140)"};
        int[] advised = {70, 95, 115}; // approx: the wiki only says to choose a route that suits your combat level and gear
        for (Route r : Route.values()) {
            String command = "/minigame trek " + f.id() + " " + r.number();
            String hover = names[r.ordinal()] + (cb < advised[r.ordinal()] ? "\nDangerous at your combat level (" + cb + ")." : "");
            line.append(Text.literal("[Route " + r.number() + "]").styled(s -> s.withColor(cb < advised[r.ordinal()] ? Formatting.RED
                            : Formatting.GOLD)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(hover)))));
            line.append(Text.literal(" "));
        }
        player.sendMessage(line, false);
    }

    /** "/minigame trek <follower> <route>": start escorting a follower standing nearby. */
    public static void choose(ServerPlayerEntity player, String followerId, int routeNumber) {
        Follower f = Follower.byId(followerId);
        Route r = Route.byNumber(routeNumber);
        if (f == null || r == null) {
            return;
        }
        boolean near = !player.getWorld().getEntitiesByClass(QuestNpcEntity.class, player.getBoundingBox().expand(10),
                e -> f.npc().equals(e.npcId())).isEmpty();
        if (!near) {
            Msg.game(player, "You need to be next to " + f.displayName + " to escort them.");
            return;
        }
        MinigameManager.enter(player, Minigame.TEMPLE_TREKKING, variant(f, r));
    }

    // ------------------------------------------------------------------ the event areas

    @Override
    protected int halfX() {
        return 26;
    }

    @Override
    protected int halfZ() {
        return 9;
    }

    @Override
    protected int height() {
        return 9;
    }

    @Override
    protected void build() {
        buildArea(null);
    }

    private void buildArea(Event e) {
        if (e == Event.BOAT) {
            // The boat to Mort'ton: a plank deck, the swamp creature surfacing around it.
            room(Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_LOG, Blocks.MOSS_BLOCK);
        } else {
            room(Blocks.MUD, Blocks.MUD_BRICKS, Blocks.MOSS_BLOCK);
        }
        for (int i = 0; i < 14; i++) {
            int x = random.nextInt(40) - 20;
            int z = random.nextInt(14) - 7;
            if (Math.abs(z) > 2) {
                set(x, 1, z, (random.nextBoolean() ? Blocks.DEAD_BUSH : Blocks.BROWN_MUSHROOM).getDefaultState());
            }
        }
        place(-25, 0, "minigame_exit");
        place(25, 0, "trek_continue");
        if (e != null && e.kind == EventType.COMBAT && route.evadeAfter >= 0) {
            place(25, -6, "trek_evade");
        }
        if (e == Event.BRIDGE || e == Event.LUMBERJACK_BRIDGE || e == Event.RIVER) {
            // A river across the path: falling in washes you back to the west bank.
            // The arena floats in the sky, so the river bed is a sealed basin below the floor.
            for (int x = -CHANNEL - 1; x <= CHANNEL + 1; x++) {
                for (int z = -halfZ() - 1; z <= halfZ() + 1; z++) {
                    boolean edge = Math.abs(x) > CHANNEL || Math.abs(z) > halfZ();
                    for (int y = -6; y <= 0; y++) {
                        if (edge) {
                            if (y < -1) {
                                set(x, y, z, Blocks.MUD_BRICKS.getDefaultState());
                            }
                        } else {
                            set(x, y, z, (y == -6 ? Blocks.MUD_BRICKS : y <= -4 ? Blocks.WATER : Blocks.AIR).getDefaultState());
                        }
                    }
                }
            }
        }
        if (e == Event.BRIDGE || e == Event.LUMBERJACK_BRIDGE) {
            place(-CHANNEL - 1, 1, "trek_bridge_gap");
        }
        if (e == Event.BRIDGE) {
            place(-12, -6, "trek_tree");
            place(-16, 6, "trek_tree");
        }
        if (e == Event.RIVER) {
            place(-10, -6, "trek_vine_tree");
            place(-14, 6, "trek_vine_tree");
            place(-CHANNEL - 1, -2, "trek_swing_tree");
        }
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayerEntity player, int i) {
        return rel(-22, 1, 0);
    }

    @Override
    protected void start() {
        events = TempleTrekking.rollEvents(route, random::nextDouble);
        String to = follower != null && follower.ramble ? "Burgh de Rott" : "Paterdomus";
        broadcast("You set off for " + to + " with " + (follower == null ? "your follower" : follower.displayName) + " on route "
                + route.number() + ".");
        nextEvent();
    }

    private void nextEvent() {
        for (LivingEntity e : living(null)) {
            if (e != companion) {
                e.discard();
            }
        }
        index++;
        if (index >= events.size()) {
            arrive();
            return;
        }
        event = events.get(index);
        spawned = 0;
        killed = 0;
        bridged = 0;
        solved = false;
        lumberjackPiece = false;
        buildArea(event);
        for (ServerPlayerEntity p : online()) {
            p.teleport(world, origin.getX() - 21.5, origin.getY() + 1, origin.getZ() + 0.5, p.getYaw(), p.getPitch());
        }
        if (companion == null || !companion.isAlive()) {
            companion = spawn(follower == null ? TempleTrekking.Difficulty.EASY.creature() : follower.difficulty.creature(), rel(-22, 1, 2));
            if (companion != null) {
                companion.setCustomName(Text.literal(follower == null ? "Follower" : follower.displayName));
                companion.setCustomNameVisible(true);
            }
        } else {
            companion.requestTeleport(origin.getX() - 21.5, origin.getY() + 1, origin.getZ() + 2.5);
        }
        broadcastRed(event.message);
        switch (event.kind) {
            case COMBAT -> {
                if (event == Event.BOAT) {
                    // One head and four tentacles, whatever the follower's difficulty.
                    MobEntity head = spawn(TempleTrekking.SWAMP_HEAD, rel(14, 1, 0));
                    if (head != null) {
                        spawned++;
                        aggro(head);
                    }
                    for (int i = 0; i < TempleTrekking.BOAT_TENTACLES; i++) {
                        MobEntity t = spawn(TempleTrekking.SWAMP_TENTACLE, rel(6 + 4 * i, 1, i % 2 == 0 ? -6 : 6));
                        if (t != null) {
                            spawned++;
                        }
                    }
                    break;
                }
                int n = (follower == null ? TempleTrekking.Difficulty.EASY : follower.difficulty).monsters(random::nextDouble);
                for (int i = 0; i < n; i++) {
                    MobEntity m = spawn(event.creature(route), randomFloor(14, 0, 6));
                    if (m != null) {
                        spawned++;
                        // Some go for the player, the rest for the follower. // approx: OSRS picks targets itself
                        if (random.nextBoolean()) {
                            aggro(m);
                        }
                    }
                }
            }
            case FRIENDLY -> abidorCrank();
            case PUZZLE -> {
                if (event == Event.BRIDGE || event == Event.LUMBERJACK_BRIDGE) {
                    // A zombie carries a bronze axe for anyone without one.
                    MobEntity z = spawn("trek_zombie", rel(-10, 1, 4));
                    if (z != null) {
                        aggro(z);
                    }
                }
            }
        }
    }

    private void abidorCrank() {
        for (ServerPlayerEntity p : online()) {
            p.heal(TempleTrekking.ABIDOR_HEAL);
            var skills = Players.data(p).skills();
            int boost = TempleTrekking.abidorDefenceBoost(skills.level(Skill.DEFENCE));
            if (skills.boost(Skill.DEFENCE) < boost) {
                skills.setBoost(Skill.DEFENCE, boost);
            }
            com.runecraft.network.ServerNetworking.sync(p);
            Msg.game(p, "Abidor Crank: There you go, that should keep you going. (+" + boost + " Defence)");
        }
        if (companion != null) {
            companion.setHealth(companion.getMaxHealth());
        }
        solved = true;
    }

    private void arrive() {
        arrived = true;
        int total = TempleTrekking.clampPoints(points);
        List<ServerPlayerEntity> list = online();
        for (ServerPlayerEntity p : list) {
            ItemStack token = Inv.stack(route.token, 1);
            if (!token.isEmpty()) {
                token.getOrCreateNbt().putInt(POINTS_NBT, total);
            }
            Inv.give(p, token);
            int kc = MinigameData.of(p).incrementStat(p.getUuid(), "kc:" + game.id());
            String name = follower == null ? "Your follower" : follower.displayName;
            Msg.game(p, name + ": Thank you for getting me here safely! Please take this token. (" + total + " points)");
            Msg.game(p, "Treks completed: " + kc + ".");
            com.runecraft.content.GameEvents.fireActivity(p, "trek_complete:"
                    + (follower == null ? TempleTrekking.Difficulty.EASY : follower.difficulty).name().toLowerCase(java.util.Locale.ROOT));
        }
        end(true);
        // Arrive at the destination rather than back where the trek started.
        boolean ramble = follower != null && follower.ramble;
        int x = GielinorMap.toBlockX(ramble ? TempleTrekking.BURGH_X : TempleTrekking.PATERDOMUS_X);
        int z = GielinorMap.toBlockZ(ramble ? TempleTrekking.BURGH_Y : TempleTrekking.PATERDOMUS_Y);
        BlockPos at = Gielinor.safeSpot(world, x, z);
        for (ServerPlayerEntity p : list) {
            if (Gielinor.is(p)) {
                p.teleport(world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p.getYaw(), p.getPitch());
            }
        }
    }

    // ------------------------------------------------------------------ ticking

    @Override
    protected void gameTick() {
        if (event == null || arrived) {
            return;
        }
        if (companion == null || !companion.isAlive()) {
            broadcastRed((follower == null ? "Your follower" : follower.displayName) + " has been killed! The trek has failed.");
            end(false);
            return;
        }
        List<ServerPlayerEntity> list = online();
        if (list.isEmpty()) {
            return;
        }
        ServerPlayerEntity p = list.get(0);
        // The follower keeps close to the player (and catches up across rivers once they're crossed).
        double d = companion.squaredDistanceTo(p);
        if (d > 144 || companion.getY() < origin.getY() - 0.5) {
            companion.requestTeleport(p.getX() - 1, p.getY(), p.getZ() + 1);
        } else if (d > 9) {
            companion.getNavigation().startMovingTo(p.getX(), p.getY(), p.getZ(), 1.0);
        }
        // Falling into the river.
        if (p.getY() < origin.getY() - 0.5) {
            p.teleport(world, origin.getX() - CHANNEL - 3 + 0.5, origin.getY() + 1, origin.getZ() + 0.5, p.getYaw(), p.getPitch());
            Msg.game(p, "You fall into the river and are washed back to the bank.");
        }
        // Monsters that aren't fighting the player go for the follower.
        for (LivingEntity e : living(null)) {
            if (e == companion || !(e instanceof MobEntity mob) || MinigameManager.creatureId(e).equals("trek_zombie")) {
                continue;
            }
            boolean engaged = mob.getTarget() instanceof ServerPlayerEntity t && t.squaredDistanceTo(mob) < 100;
            if (engaged) {
                continue;
            }
            double fd = mob.squaredDistanceTo(companion);
            if (fd > 6) {
                mob.getNavigation().startMovingTo(companion.getX(), companion.getY(), companion.getZ(), 1.0);
            } else if ((ticks + mob.getId()) % 4 == 0) {
                int max = Math.max(1, MonsterStats.of(mob).maxHit());
                mob.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                CombatHandler.applyDamage(mob, companion, random.nextInt(max + 1));
            }
        }
        // The swamp creature's head returns to full health near death while any tentacle lives.
        if (event == Event.BOAT) {
            int tentacles = living(TempleTrekking.SWAMP_TENTACLE).size();
            for (LivingEntity head : living(TempleTrekking.SWAMP_HEAD)) {
                if (TempleTrekking.headHeals(head.getHealth(), head.getMaxHealth(), tentacles)) {
                    head.setHealth(head.getMaxHealth());
                    broadcastRed("The head draws strength from its tentacles and returns to full health!");
                }
            }
        }
        // Undead lumberjacks keep wading out of the river until the bridge is fixed.
        if (event == Event.LUMBERJACK_BRIDGE && !solved && ticks % 15 == 0 && living(event.creature(route)).size() < 2) {
            MobEntity lj = spawn(event.creature(route), rel(-CHANNEL - 2, 1, random.nextInt(13) - 6));
            if (lj != null) {
                aggro(lj);
            }
        }
    }

    @Override
    protected double damageMultiplier(ServerPlayerEntity player, LivingEntity target, AttackType type) {
        if (MinigameManager.creatureId(target).startsWith("trek_vampyre_juvinate")
                && !SILVER.contains(Inv.idOf(player.getMainHandStack()))) {
            if (ticks % 5 == 0) {
                Msg.game(player, "Only silver weapons can harm the vampyre juvinate.");
            }
            return 0;
        }
        return 1.0;
    }

    @Override
    protected void onPlayerHit(ServerPlayerEntity player, LivingEntity target, int dealt) {
        // A monster the player attacks turns on them.
        if (target instanceof MobEntity mob && target != companion) {
            mob.setTarget(player);
        }
    }

    @Override
    protected void onKill(ServerPlayerEntity player, LivingEntity entity, String creatureId) {
        if (event == null || entity == companion) {
            return;
        }
        boolean boatPart = event == Event.BOAT && creatureId.equals(TempleTrekking.SWAMP_TENTACLE);
        if (event.kind == EventType.COMBAT && (creatureId.equals(event.creature(route)) || boatPart)) {
            killed++;
            points += route.killPoints;
            if (killed >= spawned) {
                solved = true;
                Msg.game(player, "You've dealt with the threat. Continue along the path.");
            }
        } else if (event == Event.LUMBERJACK_BRIDGE && creatureId.equals(event.creature(route)) && !lumberjackPiece
                && random.nextInt(TempleTrekking.LUMBERJACK_PIECE_CHANCE) == 0) {
            // One outfit piece per encounter at most, never a duplicate.
            var log = Players.data(player).collectionLog();
            List<String> missing = new ArrayList<>();
            for (String piece : TempleTrekking.LUMBERJACK) {
                if (!log.contains(piece) && !Inv.has(player, piece, 1)) {
                    missing.add(piece);
                }
            }
            if (!missing.isEmpty()) {
                lumberjackPiece = true;
                String piece = missing.get(random.nextInt(missing.size()));
                reward(player, piece, 1);
                Msg.game(player, "The undead lumberjack drops a piece of its clothing.");
            }
        }
    }

    @Override
    protected boolean onBlock(ServerPlayerEntity player, String blockId, BlockPos pos) {
        switch (blockId) {
            case "trek_continue" -> {
                if (!solved && event != null && event.kind != EventType.FRIENDLY) {
                    Msg.game(player, event.kind == EventType.COMBAT ? "You can't continue while monsters threaten your follower."
                            : "You need to find a way across first.");
                    return true;
                }
                if (event != null && event.kind == EventType.PUZZLE) {
                    points += event.points();
                }
                nextEvent();
                return true;
            }
            case "trek_evade" -> {
                if (event == null || event.kind != EventType.COMBAT || route.evadeAfter < 0) {
                    return true;
                }
                if (!route.canEvade(killed, spawned)) {
                    Msg.game(player, "You need to deal with at least half of the monsters before you can evade them.");
                    return true;
                }
                Msg.game(player, "You slip away from the fight. (No reward for this event.)");
                nextEvent();
                return true;
            }
            case "trek_tree" -> {
                if (!hasAxe(player)) {
                    Msg.game(player, "You need an axe to chop down this tree.");
                    return true;
                }
                Inv.give(player, "logs", 1);
                xp(player, Skill.WOODCUTTING, 25);
                Msg.action(player, "You get some logs.");
                return true;
            }
            case "trek_bridge_gap" -> {
                repairBridge(player);
                return true;
            }
            case "trek_vine_tree" -> {
                if (!Inv.has(player, "knife", 1)) {
                    Inv.give(player, "knife", 1);
                    Msg.game(player, "You search a nearby backpack and find a knife.");
                    return true;
                }
                if (Inv.has(player, "trek_long_vine", 1)) {
                    Msg.game(player, "You already have a long vine.");
                    return true;
                }
                Inv.give(player, "trek_vine", 1);
                Msg.action(player, "You cut a vine.");
                if (Inv.count(player, "trek_vine") >= TempleTrekking.VINES) {
                    Inv.remove(player, "trek_vine", TempleTrekking.VINES);
                    Inv.give(player, "trek_long_vine", 1);
                    Msg.game(player, "You wind the vines together into a long vine.");
                }
                return true;
            }
            case "trek_swing_tree" -> {
                if (!Inv.has(player, "trek_long_vine", 1)) {
                    Msg.game(player, "You need a long vine to swing across. Cut three vines from the small trees with a knife.");
                    return true;
                }
                Inv.remove(player, "trek_long_vine", 1);
                player.teleport(world, origin.getX() + CHANNEL + 3.5, origin.getY() + 1, origin.getZ() + 0.5, player.getYaw(),
                        player.getPitch());
                if (companion != null) {
                    companion.requestTeleport(origin.getX() + CHANNEL + 3.5, origin.getY() + 1, origin.getZ() + 2.5);
                }
                solved = true;
                Msg.game(player, "You throw the vine over the branch and swing across the river with your follower.");
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private static boolean hasAxe(ServerPlayerEntity p) {
        if (Inv.idOf(p.getMainHandStack()).endsWith("_axe")) {
            return true;
        }
        for (ItemStack s : p.getInventory().main) {
            if (Inv.idOf(s).endsWith("_axe")) {
                return true;
            }
        }
        return false;
    }

    /** Uses a log (any but pyre logs) or plank on the next gap; needs a hammer, or an axe wielded or carried. // approx */
    private void repairBridge(ServerPlayerEntity player) {
        if (solved) {
            Msg.game(player, "The bridge is already repaired.");
            return;
        }
        if (!Inv.has(player, "hammer", 1) && !hasAxe(player)) {
            Msg.game(player, "You need a hammer or an axe to repair the bridge.");
            return;
        }
        String material = null;
        for (ItemStack s : player.getInventory().main) {
            String id = Inv.idOf(s);
            if (id.equals("plank") || (id.endsWith("logs") && !id.contains("pyre"))) {
                material = id;
                break;
            }
        }
        if (material == null) {
            Msg.game(player, event == Event.BRIDGE ? "You need logs to patch the bridge. Chop the nearby trees."
                    : "You need planks to patch the bridge. The undead lumberjacks carry them.");
            return;
        }
        Inv.remove(player, material, 1);
        int x = -CHANNEL + bridged;
        set(x, 0, 0, Blocks.SPRUCE_PLANKS.getDefaultState());
        set(x, 0, 1, Blocks.SPRUCE_PLANKS.getDefaultState());
        bridged++;
        world.spawnParticles(ParticleTypes.CLOUD, origin.getX() + x + 0.5, origin.getY() + 1, origin.getZ() + 0.5, 4, 0.3, 0.2, 0.3, 0);
        if (bridged >= TempleTrekking.BRIDGE_GAPS) {
            solved = true;
            Msg.game(player, "You finish repairing the bridge.");
        } else {
            Msg.action(player, "You patch part of the bridge (" + bridged + "/" + TempleTrekking.BRIDGE_GAPS + ").");
        }
    }

    @Override
    protected void onEnd(boolean success) {
        if (!success && !arrived) {
            broadcast("The trek has been abandoned.");
        }
    }

    @Override
    protected List<String> hud(ServerPlayerEntity player) {
        List<String> lines = new ArrayList<>();
        lines.add((follower != null && follower.ramble ? "Burgh de Rott Ramble" : "Temple Trekking") + " - route " + route.number());
        lines.add("Event: " + Math.max(1, index + 1) + " / " + Math.max(1, events.size()));
        if (companion != null) {
            lines.add((follower == null ? "Follower" : follower.displayName) + ": " + (int) companion.getHealth() + " / "
                    + (int) companion.getMaxHealth());
        }
        if (event != null && event.kind == EventType.COMBAT) {
            lines.add("Monsters: " + killed + " / " + spawned);
        }
        lines.add("Points: " + TempleTrekking.clampPoints(points));
        return lines;
    }

    // ------------------------------------------------------------------ reward tokens

    /** Right-clicking a reward token: the rewards as clickable options. */
    public static void openToken(ServerPlayerEntity player, ItemStack token) {
        Route r = TempleTrekking.tokenRoute(Inv.idOf(token));
        if (r == null) {
            return;
        }
        int pts = tokenPoints(token);
        MutableText line = Text.literal("Choose a reward (" + pts + " points): ").formatted(Formatting.GRAY);
        for (Reward reward : Reward.values()) {
            List<String> choices = reward == Reward.HERBS ? List.of("herbs_tarromin", "herbs_harralander") : List.of(reward.id());
            for (String choice : choices) {
                String label = reward == Reward.TOME ? reward.displayName + " (" + r.tomeMin + "-" + r.tomeMax + " XP)"
                        : choice.equals("herbs_harralander") ? "Harralander and toadflax"
                        : choice.equals("herbs_tarromin") ? "Tarromin and toadflax"
                        : TempleTrekking.rewardCount(reward.low(r), reward.high(r), pts) + " " + reward.displayName.toLowerCase();
                String command = "/minigame token " + choice;
                line.append(Text.literal("[" + label + "]").styled(s -> s.withColor(Formatting.GOLD)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))));
                line.append(Text.literal(" "));
            }
        }
        player.sendMessage(line, false);
    }

    /** Reading a tome of experience: XP in its skill, uniform in its route's range. Returns true if the item was a tome. */
    public static boolean readTome(ServerPlayerEntity player, String id) {
        Object[] tome = TempleTrekking.tome(id);
        if (tome == null) {
            return false;
        }
        Skill skill = (Skill) tome[0];
        int xp = TempleTrekking.tomeXp((Route) tome[1], player.getRandom()::nextDouble);
        com.runecraft.skill.XpService.add(player, skill, xp);
        Msg.game(player, "You read the " + skill.displayName() + " tome and gain " + String.format("%,d", xp) + " "
                + skill.displayName() + " experience.");
        return true;
    }

    /** Temple Trekking is dangerous: dying ends the trek and is a normal death at the trek's start, items and all. */
    @Override
    protected boolean dangerous() {
        return true;
    }

    private static int tokenPoints(ItemStack token) {
        NbtCompound nbt = token.getNbt();
        return nbt != null && nbt.contains(POINTS_NBT) ? nbt.getInt(POINTS_NBT) : TempleTrekking.MIN_POINTS;
    }

    /** "/minigame token <choice>": claims the reward of the token in the player's hand. */
    public static void claim(ServerPlayerEntity player, String choice) {
        ItemStack token = player.getMainHandStack();
        Route r = TempleTrekking.tokenRoute(Inv.idOf(token));
        if (r == null) {
            Msg.game(player, "Hold the reward token you want to use.");
            return;
        }
        boolean harralander = choice.equals("herbs_harralander");
        Reward reward = choice.startsWith("herbs_") ? Reward.HERBS : Reward.byId(choice);
        if (reward == null) {
            return;
        }
        int pts = tokenPoints(token);
        token.decrement(1);
        if (reward == Reward.TOME) {
            // The skill is chosen at random (one in seven) when the tome is picked; the XP is rolled when it is read.
            Skill skill = TempleTrekking.TOME_SKILLS.get(player.getRandom().nextInt(TempleTrekking.TOME_SKILLS.size()));
            String tome = TempleTrekking.tomeId(skill, r);
            Inv.give(player, tome, 1);
            com.runecraft.collection.CollectionLog.obtain(player, tome);
            Msg.game(player, "You exchange your reward token for a " + skill.displayName() + " tome.");
            return;
        }
        for (MinigameReward m : TempleTrekking.reward(reward, r, pts, harralander)) {
            if (m.amount() > 0) {
                Inv.give(player, m.item(), m.amount());
            }
        }
        Msg.game(player, "You exchange your reward token for " + reward.displayName.toLowerCase() + ".");
    }
}
