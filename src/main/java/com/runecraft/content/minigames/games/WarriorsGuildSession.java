package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.content.systems.SysState;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameItems;
import com.runecraft.osrs.content.minigames.MinigameRules.WarriorsGuild;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestApi;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * The Warriors' Guild (OSRS Wiki), every activity an instance entered from its door in the guild:
 * <ul>
 * <li>Animation room: a full helm, platebody and platelegs of one metal on a magical animator bring animated armour to life; it
 * pays 5 (bronze) to 40 (rune) tokens and gives the pieces back (bronze 9/10 each, iron's legs and steel's helm 9/10). Armour
 * not beaten within five minutes goes back to the animator, the pieces to their owner.</li>
 * <li>Dummy room (Ajjat): dummies pop up one at a time; hitting one with its combat style or attack type gives 15 Attack XP
 * and 2 tokens, the wrong style stuns for 3 ticks.</li>
 * <li>Catapult room (Gamfred): the catapult fires every 8 ticks; blocking with the matching defence gives 10 Defence XP and 1
 * token.</li>
 * <li>Shot put room (50 Strength): tokens equal to the distance plus 1 (18lb) or 3 (22lb), Strength XP by distance.</li>
 * <li>Jimmy's challenge: balance up to five kegs on an empty head and hands for Strength XP and tokens until they fall.</li>
 * <li>Cyclops room (top floor, Kamfreena) and basement (Lorelai): 100 tokens to enter, 10 taken on entry and every minute (none
 * with an Attack cape), a minute's grace once they run out. The defender the cyclopes drop is fixed on entry from the best one
 * the player shows: the next one up to rune at 2/100 on the top floor (a new one has to be shown by re-entering), the dragon
 * defender at 1/100 in the basement, which opens once a rune defender has been shown.</li>
 * </ul>
 * Combat Achievement rewards double (easy) or triple (medium) every token payout ({@link #tokenMultiplier}).
 */
public class WarriorsGuildSession extends MinigameSession {
    /** Token multiplier (the systems module applies the Combat Achievement easy / medium reward: 2 / 3). */
    public static java.util.function.ToIntFunction<ServerPlayer> tokenMultiplier = p -> 1;

    /** The guild activity a session runs. */
    public enum Room { ANIMATOR, CYCLOPS, BASEMENT, DUMMY, CATAPULT, SHOT_PUT, KEGS }

    private final Room room;
    /** Animated armour entity id -> (owner, tier, tick it goes back to the animator). */
    private final Map<Integer, Object[]> animated = new HashMap<>();
    private long nextCharge;
    /** Per player: the defender (tier index) the cyclopes drop for them in this visit, -1 for none. */
    private final Map<UUID, Integer> target = new HashMap<>();
    /** Per player: the tick their tokens ran out (they are sent out a minute later), absent while they have tokens. */
    private final Map<UUID, Long> outOfTokens = new HashMap<>();
    /** Dummy room: the dummy positions, which one is up (-1 none) and until when; players stunned until a tick. */
    private final List<BlockPos> dummies = new ArrayList<>();
    private int dummyUp = -1;
    private String dummyType;
    private long dummyDown;
    private final java.util.Set<UUID> dummyHitBy = new java.util.HashSet<>();
    private final Map<UUID, Long> stunnedUntil = new HashMap<>();
    /** Catapult: the ammunition in flight (null none), when it lands, and each player's chosen block. */
    private String ammo;
    private long ammoLands;
    private final Map<UUID, String> block = new HashMap<>();
    /** Shot put: the shot (weight in lb) each player holds. */
    private final Map<UUID, Integer> shot = new HashMap<>();
    /** Jimmy's kegs on each player's head. */
    private final Map<UUID, Integer> kegs = new HashMap<>();

    public WarriorsGuildSession(int id, int slot, ServerLevel world, String variant) {
        super(Minigame.WARRIORS_GUILD, id, slot, world, variant);
        this.room = roomOf(variant);
    }

    /** The room an entrance block opens. */
    public static Room roomOf(String variant) {
        return switch (variant) {
            case WarriorsGuild.CYCLOPS_ENTRANCE -> Room.CYCLOPS;
            case WarriorsGuild.BASEMENT_ENTRANCE -> Room.BASEMENT;
            case WarriorsGuild.DUMMY_ENTRANCE -> Room.DUMMY;
            case WarriorsGuild.CATAPULT_ENTRANCE -> Room.CATAPULT;
            case WarriorsGuild.SHOT_PUT_ENTRANCE -> Room.SHOT_PUT;
            case WarriorsGuild.KEG_ENTRANCE -> Room.KEGS;
            default -> variant.contains("cyclops") ? Room.CYCLOPS : Room.ANIMATOR;
        };
    }

    public Room room() {
        return room;
    }

    private boolean cyclopsRoom() {
        return room == Room.CYCLOPS || room == Room.BASEMENT;
    }

    // ------------------------------------------------------------------ entry, Harrallak

    /** Whether the player wears an Attack cape (cyclopes without tokens, OSRS Wiki). */
    public static boolean attackCape(ServerPlayer p) {
        for (ItemStack s : SysState.of(p).worn.values()) {
            String id = s.isEmpty() ? "" : Inv.idOf(s);
            if (id.equals("attack_cape") || id.equals("attack_cape_t")) {
                return true;
            }
        }
        return false;
    }

    /** Index of the best defender a player shows (inventory or worn; 0 bronze .. 7 dragon), or -1. */
    public static int bestDefender(ServerPlayer p) {
        int best = -1;
        for (String tier : MinigameItems.DEFENDER_TIERS) {
            best = check(p, tier + "_defender", best);
        }
        for (String alias : new String[]{"rune_defender_t", "dragon_defender_t", "avernic_defender", "ghommals_avernic_defender_5",
                "ghommals_avernic_defender_6"}) {
            best = check(p, alias, best);
        }
        return best;
    }

    private static int check(ServerPlayer p, String item, int best) {
        if (com.runecraft.osrs.ItemCatalog.exists(item) && Inv.has(p, item, 1)) {
            return Math.max(best, WarriorsGuild.defenderTier(item));
        }
        return best;
    }

    /** Why a player can't enter a guild activity (after the Attack + Strength check), or null. */
    public static String requirement(ServerPlayer p, String variant) {
        Room r = roomOf(variant);
        boolean cape = attackCape(p);
        if ((r == Room.CYCLOPS || r == Room.BASEMENT) && !cape && !Inv.has(p, "warrior_guild_token", WarriorsGuild.CYCLOPS_ENTRY_TOKENS)) {
            return (r == Room.BASEMENT ? "Lorelai" : "Kamfreena") + ": You need at least " + WarriorsGuild.CYCLOPS_ENTRY_TOKENS
                    + " warrior guild tokens to enter.";
        }
        if (r == Room.BASEMENT && bestDefender(p) < WarriorsGuild.RUNE
                && !MinigameData.of(p).flag(p.getUUID(), WarriorsGuild.BASEMENT_FLAG)) {
            return "Lorelai: Only those who have earned a rune defender from the cyclopes upstairs may fight down here.";
        }
        if (r == Room.SHOT_PUT && Players.data(p).skills().level(Skill.STRENGTH) < WarriorsGuild.SHOT_PUT_STRENGTH) {
            return "You need a Strength level of " + WarriorsGuild.SHOT_PUT_STRENGTH + " to open the heavy door.";
        }
        return null;
    }

    private static MutableComponent link(String label, String command, String hover) {
        return Component.literal("[" + label + "]").withStyle(s -> s.withColor(ChatFormatting.GOLD)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hover))));
    }

    /** Harrallak Menarous: the guild's activities, each with a link to its room. */
    public static void harrallak(ServerPlayer p) {
        QuestApi.say(p, "Harrallak Menarous", "Welcome to the Warriors' Guild! Earn tokens in the animation room, the dummy room, the "
                + "catapult room, the shot put lanes or at Jimmy's kegs, then spend them fighting the cyclopes upstairs for defenders. "
                + "Show Lorelai a rune defender and she'll let you into the basement, where the dragon defenders are.");
        MutableComponent line = Component.literal("Activities: ").withStyle(ChatFormatting.GRAY);
        String[][] rooms = {{"animation", "Animation room"}, {"dummy", "Dummy room"}, {"catapult", "Catapult"}, {"shotput", "Shot put"},
                {"keg", "Jimmy's kegs"}, {"cyclops", "Cyclopes"}, {"basement", "Basement"}};
        for (String[] r : rooms) {
            line.append(link(r[1], "/minigame wg " + r[0], "Go to the " + r[1].toLowerCase())).append(Component.literal(" "));
        }
        p.displayClientMessage(line, false);
    }

    /** "/minigame wg <activity>" (next to Harrallak): enters an activity's room. */
    public static void enterActivity(ServerPlayer p, String activity) {
        String variant = switch (activity) {
            case "cyclops" -> WarriorsGuild.CYCLOPS_ENTRANCE;
            case "basement" -> WarriorsGuild.BASEMENT_ENTRANCE;
            case "dummy" -> WarriorsGuild.DUMMY_ENTRANCE;
            case "catapult" -> WarriorsGuild.CATAPULT_ENTRANCE;
            case "shotput" -> WarriorsGuild.SHOT_PUT_ENTRANCE;
            case "keg" -> WarriorsGuild.KEG_ENTRANCE;
            default -> Minigame.WARRIORS_GUILD.entranceBlock();
        };
        MinigameManager.enter(p, Minigame.WARRIORS_GUILD, variant);
    }

    // ------------------------------------------------------------------ the rooms

    @Override
    protected int halfX() {
        return 16;
    }

    @Override
    protected int halfZ() {
        return 16;
    }

    @Override
    protected void build() {
        room(Blocks.OAK_PLANKS, Blocks.STONE_BRICKS, Blocks.DARK_OAK_PLANKS);
        switch (room) {
            case ANIMATOR -> {
                place(-8, -8, "wg_armour_animator");
                place(8, -8, "wg_armour_animator");
                place(-8, 0, "wg_armour_animator");
                place(8, 0, "wg_armour_animator");
            }
            case DUMMY -> {
                for (int i = 0; i < WarriorsGuild.DUMMIES.size(); i++) {
                    BlockPos at = rel(-9 + i * 3, 1, -4);
                    dummies.add(at);
                    set(-9 + i * 3, 0, -4, Blocks.HAY_BLOCK.defaultBlockState());
                }
            }
            case CATAPULT -> place(0, -12, "wg_catapult");
            case SHOT_PUT -> {
                place(-4, 6, "wg_shot_18");
                place(4, 6, "wg_shot_22");
                for (int z = -12; z <= 4; z++) {
                    set(-4, 0, z, Blocks.SMOOTH_STONE.defaultBlockState());
                    set(4, 0, z, Blocks.SMOOTH_STONE.defaultBlockState());
                }
            }
            case KEGS -> {
                for (int i = 0; i < WarriorsGuild.MAX_KEGS; i++) {
                    place(-6 + i * 3, -6, "wg_keg");
                }
            }
            default -> {
            }
        }
        place(0, 14, "minigame_exit");
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayer player, int index) {
        return rel(index % 3 - 1, 1, 11);
    }

    @Override
    protected void start() {
        for (ServerPlayer p : online()) {
            switch (room) {
                case CYCLOPS, BASEMENT -> enterCyclopes(p);
                case DUMMY -> Msg.game(p, "Ajjat: Hit each dummy with the attack style written on it: Accurate, Aggressive, Controlled "
                        + "or Defensive, or a Stab, Slash or Crush weapon.");
                case CATAPULT -> Msg.game(p, "Gamfred: Take this defensive shield and block what the catapult throws at you: stab, "
                        + "blunt, slash or magic.");
                case SHOT_PUT -> Msg.game(p, "Pick up a shot from a pile and throw it down the lane. The 22lb shot pays more tokens.");
                case KEGS -> Msg.game(p, "Jimmy: Balance the kegs on your head, then! My record's five.");
                default -> {
                }
            }
        }
        if (cyclopsRoom()) {
            nextCharge = WarriorsGuild.CYCLOPS_TICKS_PER_CHARGE;
        }
    }

    private void enterCyclopes(ServerPlayer p) {
        int best = bestDefender(p);
        boolean basement = room == Room.BASEMENT;
        if (basement && best >= WarriorsGuild.RUNE) {
            MinigameData.of(p).setFlag(p.getUUID(), WarriorsGuild.BASEMENT_FLAG);
        }
        int t = WarriorsGuild.targetDefender(best, basement);
        target.put(p.getUUID(), t);
        String keeper = basement ? "Lorelai" : "Kamfreena";
        if (!attackCape(p)) {
            Inv.remove(p, "warrior_guild_token", WarriorsGuild.CYCLOPS_TOKENS);
            Msg.game(p, WarriorsGuild.CYCLOPS_TOKENS + " tokens are taken as you enter, then " + WarriorsGuild.CYCLOPS_TOKENS
                    + " every minute.");
        }
        Msg.game(p, keeper + ": The cyclopes will drop " + defenderName(t) + "s for you.");
    }

    private static String defenderName(int tier) {
        String t = MinigameItems.DEFENDER_TIERS[Math.max(0, tier)];
        return Character.toUpperCase(t.charAt(0)) + t.substring(1) + " defender";
    }

    /** Self-test hook: spawns one of the room's cyclopes at once (the room keeps 4 + players and adds one every 10 ticks). */
    public Mob selfTestSpawnCyclops() {
        String kind = room == Room.BASEMENT ? "cyclops_elite" : random.nextBoolean() ? "warriors_guild_cyclops" : WarriorsGuild.CYCLOPS_76;
        return spawn(kind, randomFloor(0, -6, 8));
    }

    /** Where the first magical animator stands (public for the self-test). */
    public BlockPos animatorAt() {
        return rel(-8, 1, -8);
    }

    /** The defender the cyclopes drop for a player in this visit, -1 for none (public for the self-test). */
    public int target(ServerPlayer p) {
        return target.getOrDefault(p.getUUID(), -1);
    }

    @Override
    protected void gameTick() {
        switch (room) {
            case CYCLOPS, BASEMENT -> cyclopsTick();
            case ANIMATOR -> animatorTick();
            case DUMMY -> dummyTick();
            case CATAPULT -> catapultTick();
            case KEGS -> kegTick();
            default -> {
            }
        }
    }

    private void cyclopsTick() {
        if (ticks >= nextCharge) {
            nextCharge = ticks + WarriorsGuild.CYCLOPS_TICKS_PER_CHARGE;
            for (ServerPlayer p : online()) {
                if (attackCape(p) || outOfTokens.containsKey(p.getUUID())) {
                    continue;
                }
                if (Inv.remove(p, "warrior_guild_token", WarriorsGuild.CYCLOPS_TOKENS) < WarriorsGuild.CYCLOPS_TOKENS) {
                    outOfTokens.put(p.getUUID(), ticks);
                    Msg.red(p, "You have run out of tokens. You have a minute left before you're asked to leave.");
                }
            }
        }
        for (ServerPlayer p : online()) {
            Long out = outOfTokens.get(p.getUUID());
            if (out != null && ticks - out >= WarriorsGuild.CYCLOPS_GRACE_TICKS) {
                Msg.red(p, (room == Room.BASEMENT ? "Lorelai" : "Kamfreena") + ": Time's up! Come back with more tokens.");
                removePlayer(p, true);
            }
        }
        if (isEnded()) {
            return;
        }
        List<LivingEntity> cyclopes = living(null);
        if (ticks % 10 == 0 && cyclopes.size() < 4 + playerCount()) {
            String kind = room == Room.BASEMENT ? "cyclops_elite" : random.nextBoolean() ? "warriors_guild_cyclops" : WarriorsGuild.CYCLOPS_76;
            Mob c = spawn(kind, randomFloor(0, -6, 8));
            if (c != null) {
                aggro(c);
            }
        }
    }

    // ------------------------------------------------------------------ animation room

    private void animatorTick() {
        for (Map.Entry<Integer, Object[]> e : new ArrayList<>(animated.entrySet())) {
            Object[] a = e.getValue();
            if (ticks < (Long) a[2]) {
                continue;
            }
            animated.remove(e.getKey());
            net.minecraft.world.entity.Entity armour = world.getEntity(e.getKey());
            if (armour != null) {
                armour.discard();
            }
            ServerPlayer owner = world.getServer().getPlayerList().getPlayer((UUID) a[0]);
            if (owner != null) {
                givePieces(owner, (String) a[1], false);
                Msg.game(owner, "Your animated armour wasn't defeated in time and falls apart; Shanomi hands you the pieces back.");
            }
        }
    }

    private void givePieces(ServerPlayer p, String tier, boolean roll) {
        for (String piece : new String[]{"full_helm", "platebody", "platelegs"}) {
            if (!roll || random.nextInt(10) < WarriorsGuild.returnTenths(tier, piece)) {
                Inv.give(p, tier + "_" + piece, 1);
            } else {
                Msg.red(p, "Your " + tier + " " + piece.replace('_', ' ') + " was destroyed in the fight.");
            }
        }
    }

    // ------------------------------------------------------------------ dummy room

    private void dummyTick() {
        if (dummyUp >= 0 && ticks >= dummyDown) {
            set(dummies.get(dummyUp).getX() - origin.getX(), 1, dummies.get(dummyUp).getZ() - origin.getZ(), Blocks.AIR.defaultBlockState());
            dummyUp = -1;
        }
        if (dummyUp < 0 && ticks % WarriorsGuild.DUMMY_INTERVAL == 0) {
            popDummy(random.nextInt(dummies.size()), WarriorsGuild.DUMMIES.get(random.nextInt(WarriorsGuild.DUMMIES.size())));
        }
    }

    /** Raises a dummy needing {@code type} at position {@code index} (public for the self-test). */
    public void popDummy(int index, String type) {
        if (dummyUp >= 0) {
            set(dummies.get(dummyUp).getX() - origin.getX(), 1, dummies.get(dummyUp).getZ() - origin.getZ(), Blocks.AIR.defaultBlockState());
        }
        dummyUp = index;
        dummyType = type;
        dummyDown = ticks + WarriorsGuild.DUMMY_UP_TICKS;
        dummyHitBy.clear();
        BlockPos at = dummies.get(index);
        place(at.getX() - origin.getX(), at.getZ() - origin.getZ(), "wg_dummy");
        broadcast("A dummy marked '" + Character.toUpperCase(type.charAt(0)) + type.substring(1) + "' pops up!");
    }

    /** Where the dummy that is up stands, or null (public for the self-test). */
    public BlockPos dummyUp() {
        return dummyUp < 0 ? null : dummies.get(dummyUp);
    }

    private void hitDummy(ServerPlayer p, BlockPos pos) {
        if (dummyUp < 0 || !dummies.get(dummyUp).equals(pos)) {
            Msg.game(p, "The dummy has gone back down.");
            return;
        }
        if (ticks < stunnedUntil.getOrDefault(p.getUUID(), 0L) || !dummyHitBy.add(p.getUUID())) {
            return;
        }
        String style = Players.data(p).style().displayName();
        String type = com.runecraft.combat.Equipment.meleeType(p, Players.data(p).skills()).name();
        if (WarriorsGuild.dummyHit(dummyType, style, type)) {
            xp(p, Skill.ATTACK, WarriorsGuild.DUMMY_XP);
            tokens(p, WarriorsGuild.DUMMY_TOKENS);
            world.sendParticles(ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 10, 0.3, 0.4, 0.3, 0.2);
        } else {
            stunnedUntil.put(p.getUUID(), ticks + WarriorsGuild.DUMMY_STUN_TICKS);
            Msg.red(p, "Wrong style! The dummy needed " + dummyType + "; you're stunned for a moment.");
        }
    }

    // ------------------------------------------------------------------ catapult

    private void catapultTick() {
        if (ammo != null && ticks >= ammoLands) {
            for (ServerPlayer p : online()) {
                if (ammo.equals(block.get(p.getUUID()))) {
                    xp(p, Skill.DEFENCE, WarriorsGuild.CATAPULT_XP);
                    tokens(p, WarriorsGuild.CATAPULT_TOKENS);
                    Msg.game(p, "You block the " + ammo + " shot.");
                } else {
                    hurt(p, WarriorsGuild.CATAPULT_DAMAGE);
                    Msg.red(p, "The " + ammo + " shot gets past your shield!");
                }
            }
            block.clear();
            ammo = null;
        }
        if (ammo == null && ticks % WarriorsGuild.CATAPULT_INTERVAL == 0) {
            fire(WarriorsGuild.CATAPULT_AMMO.get(random.nextInt(WarriorsGuild.CATAPULT_AMMO.size())));
        }
    }

    /** The catapult fires {@code type}; it lands a few ticks later (public for the self-test). */
    public void fire(String type) {
        ammo = type;
        ammoLands = ticks + WarriorsGuild.CATAPULT_INTERVAL - 2;
        block.clear();
        for (ServerPlayer p : online()) {
            MutableComponent line = Component.literal("The catapult fires a " + type + " shot! Block: ").withStyle(ChatFormatting.YELLOW);
            for (String b : WarriorsGuild.CATAPULT_AMMO) {
                line.append(link(Character.toUpperCase(b.charAt(0)) + b.substring(1), "/minigame act block " + b, "Defend against " + b))
                        .append(Component.literal(" "));
            }
            p.displayClientMessage(line, false);
        }
    }

    // ------------------------------------------------------------------ shot put

    private void throwShot(ServerPlayer p, String style) {
        Integer weight = shot.remove(p.getUUID());
        if (weight == null) {
            Msg.game(p, "Pick up a shot from one of the piles first.");
            return;
        }
        if (!WarriorsGuild.SHOT_STYLES.contains(style)) {
            style = "standing";
        }
        SysState.SysPlayer sp = SysState.of(p);
        int energy = sp.runEnergy / 100;
        if (random.nextInt(WarriorsGuild.SHOT_FAIL_CHANCE) == 0) {
            hurt(p, 1);
            Msg.red(p, "The shot slips from your hand and lands on your toe!");
            return;
        }
        int x = WarriorsGuild.shotX(Players.data(p).skills().level(Skill.STRENGTH), energy, style, false, weight);
        int yards = WarriorsGuild.shotYards(x);
        // A throw takes a tenth of the player's run energy. // approx
        sp.runEnergy = Math.max(0, sp.runEnergy - 1000);
        xp(p, Skill.STRENGTH, WarriorsGuild.shotXp(yards, x));
        tokens(p, WarriorsGuild.shotTokens(yards, weight));
        Msg.game(p, "You throw the " + weight + "lb shot " + yards + " yard" + (yards == 1 ? "" : "s") + ".");
    }

    // ------------------------------------------------------------------ Jimmy's kegs

    private static boolean kegReady(ServerPlayer p) {
        return p.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && p.getMainHandItem().isEmpty() && p.getOffhandItem().isEmpty();
    }

    private void kegTick() {
        if (ticks % WarriorsGuild.KEG_INTERVAL != 0) {
            return;
        }
        for (ServerPlayer p : online()) {
            int n = kegs.getOrDefault(p.getUUID(), 0);
            if (n <= 0) {
                continue;
            }
            int energy = SysState.of(p).runEnergy / 100;
            if (random.nextDouble() < WarriorsGuild.kegFallChance(energy, n)) {
                kegs.remove(p.getUUID());
                hurt(p, 1 + random.nextInt(WarriorsGuild.KEG_MAX_DAMAGE));
                Msg.red(p, "The kegs tumble off your head!");
                continue;
            }
            xp(p, Skill.STRENGTH, WarriorsGuild.KEG_XP * n);
            tokens(p, WarriorsGuild.KEG_TOKENS * n);
        }
    }

    /** Kegs on a player's head (public for the self-test). */
    public int kegs(ServerPlayer p) {
        return kegs.getOrDefault(p.getUUID(), 0);
    }

    // ------------------------------------------------------------------ clicks and kills

    private void tokens(ServerPlayer p, int base) {
        reward(p, "warrior_guild_token", base * Math.max(1, tokenMultiplier.applyAsInt(p)));
    }

    @Override
    protected boolean onBlock(ServerPlayer player, String blockId, BlockPos pos) {
        switch (blockId) {
            case "wg_armour_animator" -> {
                animate(player, pos);
                return true;
            }
            case "wg_dummy" -> {
                hitDummy(player, pos);
                return true;
            }
            case "wg_catapult" -> {
                Msg.game(player, "Gamfred: Stand back and block! Pick the defence that matches what it throws.");
                return true;
            }
            case "wg_shot_18", "wg_shot_22" -> {
                int weight = blockId.equals("wg_shot_22") ? 22 : 18;
                shot.put(player.getUUID(), weight);
                MutableComponent line = Component.literal("You pick up a " + weight + "lb shot. Throw: ").withStyle(ChatFormatting.GRAY);
                line.append(link("Standing throw", "/minigame act throw standing", "Style factor 20"))
                        .append(Component.literal(" ")).append(link("Step and throw", "/minigame act throw step", "Style factor 10"))
                        .append(Component.literal(" ")).append(link("Spin and throw", "/minigame act throw spin", "Style factor 0"));
                player.displayClientMessage(line, false);
                return true;
            }
            case "wg_keg" -> {
                if (!kegReady(player)) {
                    Msg.game(player, "Jimmy: You need your head and both hands free to balance kegs!");
                    return true;
                }
                int n = kegs.getOrDefault(player.getUUID(), 0);
                if (n >= WarriorsGuild.MAX_KEGS) {
                    Msg.game(player, "Jimmy: Five's the record. You can't balance any more!");
                    return true;
                }
                SysState.SysPlayer sp = SysState.of(player);
                sp.runEnergy = Math.max(0, sp.runEnergy - WarriorsGuild.KEG_ENERGY);
                kegs.put(player.getUUID(), n + 1);
                Msg.game(player, "You balance " + (n + 1) + " keg" + (n == 0 ? "" : "s") + " on your head.");
                if (n + 1 == WarriorsGuild.MAX_KEGS) {
                    Msg.game(player, "Jimmy: Five! You've matched my record!");
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    protected boolean onAction(ServerPlayer player, String action, String arg) {
        if (action.equals("block") && room == Room.CATAPULT && arg != null && WarriorsGuild.CATAPULT_AMMO.contains(arg)) {
            block.put(player.getUUID(), arg);
            return true;
        }
        if (action.equals("throw") && room == Room.SHOT_PUT) {
            throwShot(player, arg == null ? "standing" : arg);
            return true;
        }
        return false;
    }

    private void animate(ServerPlayer player, BlockPos pos) {
        for (int i = WarriorsGuild.ARMOUR.length - 1; i >= 0; i--) {
            String tier = (String) WarriorsGuild.ARMOUR[i][0];
            if (Inv.has(player, tier + "_full_helm", 1) && Inv.has(player, tier + "_platebody", 1)
                    && Inv.has(player, tier + "_platelegs", 1)) {
                Inv.remove(player, tier + "_full_helm", 1);
                Inv.remove(player, tier + "_platebody", 1);
                Inv.remove(player, tier + "_platelegs", 1);
                Mob armour = spawn("animated_" + tier + "_armour", pos.above().south(2));
                if (armour != null) {
                    armour.setTarget(player);
                    animated.put(armour.getId(), new Object[]{player.getUUID(), tier, ticks + WarriorsGuild.ANIMATED_TICKS});
                    world.sendParticles(ParticleTypes.ENCHANT, armour.getX(), armour.getY() + 1, armour.getZ(), 30, 0.5, 1, 0.5, 0.5);
                    Msg.game(player, "You place your armour on the platform where it disappears... The animator hums, something appears!");
                } else {
                    givePieces(player, tier, false);
                }
                return;
            }
        }
        Msg.game(player, "You need a full helm, platebody and platelegs of the same metal in your inventory.");
    }

    @Override
    protected void onKill(ServerPlayer player, LivingEntity entity, String creatureId) {
        Object[] a = animated.remove(entity.getId());
        if (a != null) {
            String tier = (String) a[1];
            ServerPlayer owner = world.getServer().getPlayerList().getPlayer((UUID) a[0]);
            givePieces(owner != null ? owner : player, tier, true);
            int base = WarriorsGuild.tokens(tier);
            tokens(player, base);
            Msg.game(player, "The animated armour collapses. You collect " + base * Math.max(1, tokenMultiplier.applyAsInt(player))
                    + " tokens.");
            return;
        }
        if (!creatureId.contains("cyclops")) {
            return;
        }
        int t = target(player);
        if (t < 0 || random.nextInt(WarriorsGuild.defenderChance(t)) != 0) {
            return;
        }
        dropDefender(player, t);
    }

    /**
     * A cyclops drops the player's target defender: on the top floor a new one has to be shown by re-entering before the next
     * tier drops (OSRS Wiki); the basement keeps dropping dragon defenders. Public for the self-test.
     */
    public void dropDefender(ServerPlayer player, int tier) {
        String item = MinigameItems.DEFENDER_TIERS[tier] + "_defender";
        reward(player, item, 1);
        Msg.red(player, "The cyclops dropped a " + item.replace('_', ' ') + "!");
        if (room == Room.CYCLOPS) {
            target.put(player.getUUID(), -1);
            Msg.game(player, "Kamfreena: Well done! Show me your new defender by coming back in, and the cyclopes will drop the next one.");
        }
    }

    @Override
    protected void onLeave(ServerPlayer player) {
        for (Object[] a : new ArrayList<>(animated.values())) {
            if (player.getUUID().equals(a[0])) {
                givePieces(player, (String) a[1], false);
            }
        }
        animated.values().removeIf(a -> player.getUUID().equals(a[0]));
        if (kegs.remove(player.getUUID()) != null) {
            Msg.game(player, "The kegs vanish as you leave the room.");
        }
        shot.remove(player.getUUID());
        outOfTokens.remove(player.getUUID());
        target.remove(player.getUUID());
    }

    @Override
    protected List<String> hud(ServerPlayer player) {
        String tokens = "Tokens: " + Inv.count(player, "warrior_guild_token");
        return switch (room) {
            case CYCLOPS, BASEMENT -> {
                int best = bestDefender(player);
                int t = target(player);
                yield List.of(room == Room.BASEMENT ? "Warriors' Guild - Basement" : "Warriors' Guild - Cyclops room", tokens,
                        attackCape(player) ? "Attack cape: no tokens needed" : "Next charge: " + time(nextCharge - ticks),
                        "Best defender: " + (best < 0 ? "none" : MinigameItems.DEFENDER_TIERS[best]),
                        "Cyclopes drop: " + (t < 0 ? "nothing (show your new defender)" : defenderName(t)));
            }
            case DUMMY -> List.of("Warriors' Guild - Dummy room", tokens, dummyUp < 0 ? "Wait for a dummy" : "Dummy: " + dummyType);
            case CATAPULT -> List.of("Warriors' Guild - Catapult", tokens, ammo == null ? "Get ready..." : "Incoming: " + ammo);
            case SHOT_PUT -> List.of("Warriors' Guild - Shot put", tokens, "Run energy: " + SysState.of(player).runEnergy / 100 + "%");
            case KEGS -> List.of("Warriors' Guild - Jimmy's challenge", tokens, "Kegs: " + kegs(player) + " / " + WarriorsGuild.MAX_KEGS,
                    "Run energy: " + SysState.of(player).runEnergy / 100 + "%");
            default -> List.of("Warriors' Guild - Animator room", tokens, "Place armour on an animator");
        };
    }
}
