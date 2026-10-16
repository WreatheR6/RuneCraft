package com.runecraft.content.minigames.games;

import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.CombatHooks;
import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.content.minigames.RewardShopService;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameCurrencies;
import com.runecraft.osrs.content.minigames.MinigameRules.NightmareZone;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestApi;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Nightmare Zone: Dominic Onion's dreams pit the player against the quest bosses they have beaten (OSRS Wiki). Four modes, each
 * normal or hard: practice (one chosen boss, free, no points), endurance (every unlocked boss in turn, 1-on-1; 1,000 / 5,000
 * coins), rumble (up to four bosses at once, endlessly; 2,000 / 6,000) and customisable rumble (bosses can be switched off;
 * 22,000 / 26,000). Points per kill are the wiki's per-boss values scaled by how many bosses the dream includes. Hard bosses
 * have the wiki's hard hitpoints, attack levels and max hits. Power-ups appear in rumbles (recurrent damage, zapper, power surge,
 * ultimate force). Dom Onion's Reward Shop sells the dream potions by the dose into the lobby's potion barrels (255 doses each),
 * from which any number of doses is withdrawn; they only work inside a dream and are taken away on waking. Death is safe.
 */
public class NightmareZoneSession extends MinigameSession {
    private static final Skill[] OVERLOAD_SKILLS = {Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC};
    /** Command tag on bosses of a hard dream (their attacks get the hard levels and max hits). */
    public static final String HARD_TAG = "rc_nmz_hard";
    private static final String OFF_PREFIX = "nmz_off:";
    private static final String BARREL_PREFIX = "nmz_barrel:";

    private final NightmareZone.Dream dream;
    private final boolean hard;
    /** The bosses this dream draws from, and how far an endurance run has got. */
    private List<String> pool = new ArrayList<>();
    private int next;
    private boolean practiceSpawned;
    /**
     * The bosses spawned, tracked directly: box queries miss mobs in an instance chunk that isn't ticking yet (the
     * self-test's fake player loads no chunks).
     */
    private final List<MobEntity> spawnedBosses = new ArrayList<>();
    private int kills;
    private int earned;
    /** Absorption points per player. */
    private final Map<UUID, Integer> absorption = new HashMap<>();
    /** Game tick each player's overload started (absent when none is active). */
    private final Map<UUID, Long> overload = new HashMap<>();
    /** Active power-up per player and the tick it wears off. */
    private final Map<UUID, NightmareZone.PowerUp> power = new HashMap<>();
    private final Map<UUID, Long> powerUntil = new HashMap<>();
    /** Bosses a player has hit at least once (the zapper may then finish Count Draynor). */
    private final Set<UUID> hitByPlayer = new HashSet<>();
    /** The power-up waiting in the arena (null when none), where, and the tick it vanishes. */
    private NightmareZone.PowerUp spawned;
    private BlockPos spawnedAt;
    private long spawnedUntil;
    private long nextPowerUp = NightmareZone.POWER_UP_INTERVAL;
    /** Ultimate force kills pay no points. */
    private boolean forcing;

    public NightmareZoneSession(int id, int slot, ServerWorld world, String variant) {
        super(Minigame.NIGHTMARE_ZONE, id, slot, world, variant);
        this.dream = NightmareZone.Dream.parse(variant);
        this.hard = dream.hard();
    }

    // ------------------------------------------------------------------ choosing a dream

    private static boolean questDone(ServerPlayerEntity player, String quest) {
        return ContentRegistry.quest(quest) == null || QuestApi.isComplete(player, quest);
    }

    /** The bosses a player has unlocked (by quest), in the wiki table's order. */
    public static List<String> unlocked(ServerPlayerEntity player) {
        return NightmareZone.unlocked(q -> questDone(player, q));
    }

    /** The bosses a customisable rumble would use: the unlocked ones the player hasn't switched off. */
    public static List<String> customPool(ServerPlayerEntity player) {
        List<String> out = new ArrayList<>();
        MinigameData data = MinigameData.of(player);
        for (String boss : unlocked(player)) {
            if (data.stat(player.getUuid(), OFF_PREFIX + boss) == 0) {
                out.add(boss);
            }
        }
        return out;
    }

    /** Entry check: five boss quests, the dream's fee in coins, and a valid boss choice. */
    public static String requirement(ServerPlayerEntity player, String variant) {
        if (!NightmareZone.canEnter(q -> ContentRegistry.quest(q) != null && QuestApi.isComplete(player, q))) {
            return "Dominic Onion: Your brain is insufficiently trained for my dreams. Complete at least "
                    + NightmareZone.QUESTS_NEEDED + " quests with bosses in them first.";
        }
        NightmareZone.Dream d = NightmareZone.Dream.parse(variant);
        if (d.mode() == NightmareZone.Mode.PRACTICE && (d.boss() == null || !unlocked(player).contains(d.boss()))) {
            return "Dominic Onion: You can only practise against a boss you have defeated in a quest.";
        }
        if (d.mode() == NightmareZone.Mode.CUSTOM_RUMBLE && customPool(player).size() < Math.min(5, unlocked(player).size())) {
            return "Dominic Onion: A customisable rumble needs at least five bosses switched on.";
        }
        int cost = d.mode().cost(d.hard());
        if (cost > 0 && !Inv.has(player, "coins", cost)) {
            return "Dominic Onion: A " + (d.hard() ? "hard " : "") + d.mode().displayName.toLowerCase() + " dream costs "
                    + String.format("%,d", cost) + " coins.";
        }
        return null;
    }

    private static String bossName(String id) {
        var def = ContentRegistry.monster(id);
        return def == null ? id : def.name;
    }

    private static MutableText option(String label, String command, String hover, Formatting colour) {
        return Text.literal("[" + label + "]").styled(s -> s.withColor(colour)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(hover))));
    }

    /** Dominic Onion: the four dream modes (normal or hard) as clickable options, and his reward shop. */
    public static void dominic(ServerPlayerEntity player) {
        QuestApi.say(player, "Dominic Onion", "Welcome to the Nightmare Zone! Which dream would you like? Practice is free; the others "
                + "cost coins, and hard dreams make the bosses far stronger.");
        for (NightmareZone.Mode m : NightmareZone.Mode.values()) {
            MutableText line = Text.literal(m.displayName + ": ").formatted(Formatting.GRAY);
            if (m == NightmareZone.Mode.PRACTICE) {
                line.append(option("Choose a boss", "/minigame nmz practice normal", "Fight one boss you have beaten, for no points.",
                        Formatting.GOLD));
            } else {
                for (boolean h : new boolean[]{false, true}) {
                    line.append(option(h ? "Hard" : "Normal", "/minigame nmz " + m.id + " " + (h ? "hard" : "normal"),
                            String.format("%,d", m.cost(h)) + " coins", h ? Formatting.RED : Formatting.GOLD));
                    line.append(Text.literal(" "));
                }
                if (m == NightmareZone.Mode.CUSTOM_RUMBLE) {
                    line.append(option("Bosses", "/minigame nmz bosses", "Switch bosses on or off.", Formatting.AQUA));
                }
            }
            player.sendMessage(line, false);
        }
        player.sendMessage(option("Rewards", "/minigame nmz shop", "Dom Onion's Reward Shop", Formatting.GREEN), false);
    }

    /** "/minigame nmz <mode> <normal|hard> [boss]": start a dream, or list the practice bosses. */
    public static void choose(ServerPlayerEntity player, String modeId, String difficulty, String boss) {
        NightmareZone.Mode mode = NightmareZone.Mode.byId(modeId);
        if (mode == null) {
            return;
        }
        boolean h = difficulty.equals("hard");
        if (mode == NightmareZone.Mode.PRACTICE && boss == null) {
            MutableText line = Text.literal("Practise against: ").formatted(Formatting.GRAY);
            for (String b : unlocked(player)) {
                line.append(option(bossName(b), "/minigame nmz practice normal " + b, "Normal", Formatting.GOLD));
                line.append(option("H", "/minigame nmz practice hard " + b, "Hard", Formatting.RED));
                line.append(Text.literal(" "));
            }
            player.sendMessage(line, false);
            return;
        }
        MinigameManager.enter(player, Minigame.NIGHTMARE_ZONE, new NightmareZone.Dream(mode, h,
                mode == NightmareZone.Mode.PRACTICE ? boss : null).variant());
    }

    /** "/minigame nmz bosses" and "/minigame nmz toggle <boss>": the customisable rumble's boss switches. */
    public static void toggle(ServerPlayerEntity player, String boss) {
        MinigameData data = MinigameData.of(player);
        if (boss != null && unlocked(player).contains(boss)) {
            String key = OFF_PREFIX + boss;
            data.setStat(player.getUuid(), key, data.stat(player.getUuid(), key) == 0 ? 1 : 0);
        }
        MutableText line = Text.literal("Customisable rumble bosses: ").formatted(Formatting.GRAY);
        for (String b : unlocked(player)) {
            boolean on = data.stat(player.getUuid(), OFF_PREFIX + b) == 0;
            line.append(option(bossName(b), "/minigame nmz toggle " + b, on ? "On: click to switch off" : "Off: click to switch on",
                    on ? Formatting.GREEN : Formatting.DARK_GRAY));
            line.append(Text.literal(" "));
        }
        player.sendMessage(line, false);
    }

    // ------------------------------------------------------------------ potion barrels

    /** Doses of a potion stored in the player's barrel. */
    public static int barrel(ServerPlayerEntity player, String potion) {
        return MinigameData.of(player).stat(player.getUuid(), BARREL_PREFIX + potion);
    }

    /** Doses that fit in the barrel; called by the reward shop before a purchase. */
    public static int barrelRoom(ServerPlayerEntity player, String potion, int wanted) {
        return NightmareZone.barrelRoom(barrel(player, potion), wanted);
    }

    /** Stores bought doses in the barrel (the reward shop's purchase of a dream potion). */
    public static void storeDoses(ServerPlayerEntity player, String potion, int doses) {
        MinigameData.of(player).setStat(player.getUuid(), BARREL_PREFIX + potion,
                Math.min(NightmareZone.BARREL_CAPACITY, barrel(player, potion) + Math.max(0, doses)));
    }

    /** The potion barrels: doses stored and withdraw options. */
    public static void barrels(ServerPlayerEntity player) {
        Msg.game(player, "--- Nightmare Zone potion barrels ---");
        for (String potion : NightmareZone.POTIONS) {
            var def = com.runecraft.osrs.ItemCatalog.get(potion);
            int n = barrel(player, potion);
            MutableText line = Text.literal((def == null ? potion : def.name()) + ": " + n + " doses ").formatted(Formatting.GRAY);
            for (int take : new int[]{1, 4, 16, 28}) {
                line.append(option(String.valueOf(take), "/minigame nmz barrel " + potion + " " + take, "Withdraw " + take + " doses",
                        Formatting.GOLD));
            }
            line.append(option("All", "/minigame nmz barrel " + potion + " " + NightmareZone.BARREL_CAPACITY, "Withdraw every dose",
                    Formatting.GOLD));
            player.sendMessage(line, false);
        }
    }

    /** "/minigame nmz barrel <potion> <doses>": withdraws doses as 4-dose potions plus one partly drunk potion. */
    public static int withdraw(ServerPlayerEntity player, String potion, int doses) {
        if (!NightmareZone.POTIONS.contains(potion)) {
            return 0;
        }
        int take = Math.min(Math.max(0, doses), barrel(player, potion));
        if (take <= 0) {
            Msg.game(player, "That barrel is empty. Buy doses at Dom Onion's Reward Shop.");
            return 0;
        }
        MinigameData.of(player).setStat(player.getUuid(), BARREL_PREFIX + potion, barrel(player, potion) - take);
        int left = take;
        while (left > 0) {
            int d = Math.min(4, left);
            ItemStack s = Inv.stack(potion, 1);
            if (!s.isEmpty()) {
                s.setDamage(4 - d);
                Inv.give(player, s);
            }
            left -= d;
        }
        ServerNetworking.sync(player);
        Msg.game(player, "You withdraw " + take + " dose" + (take == 1 ? "" : "s") + " from the barrel.");
        return take;
    }

    /** "/minigame nmz shop": Dom Onion's Reward Shop. */
    public static void shop(ServerPlayerEntity player) {
        RewardShopService.open(player, "nightmare_zone");
    }

    // ------------------------------------------------------------------ the arena

    @Override
    protected int halfX() {
        return 18;
    }

    @Override
    protected int halfZ() {
        return 18;
    }

    @Override
    protected void build() {
        room(Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.CRYING_OBSIDIAN);
        place(0, 16, "minigame_exit");
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayerEntity player, int index) {
        return rel(index % 3 - 1, 1, 12);
    }

    /** The dream being played (public for the self-test). */
    public NightmareZone.Dream dream() {
        return dream;
    }

    /** The bosses this dream draws from (public for the self-test). */
    public List<String> pool() {
        return pool;
    }

    @Override
    protected void start() {
        int cost = dream.mode().cost(hard);
        List<ServerPlayerEntity> list = online();
        for (ServerPlayerEntity p : list) {
            if (cost > 0) {
                Inv.remove(p, "coins", cost);
            }
            Msg.game(p, "You drink the dream potion and drift into a " + (hard ? "hard " : "") + dream.mode().displayName.toLowerCase()
                    + " dream...");
        }
        if (dream.mode() == NightmareZone.Mode.PRACTICE) {
            pool = new ArrayList<>(List.of(dream.boss() != null ? dream.boss() : (String) NightmareZone.BOSSES[0][0]));
        } else if (list.isEmpty()) {
            pool = NightmareZone.unlocked(q -> false); // the self-test's fake player: every boss
        } else if (dream.mode() == NightmareZone.Mode.CUSTOM_RUMBLE) {
            pool = customPool(list.get(0));
        } else {
            pool = unlocked(list.get(0));
        }
        if (pool.isEmpty()) {
            pool = NightmareZone.unlocked(q -> false);
        }
    }

    private MobEntity spawnBoss(String id) {
        MobEntity mob = spawn(id, randomFloor(0, -6, 10));
        if (mob == null) {
            return null;
        }
        spawnedBosses.add(mob);
        if (hard) {
            // Hard rumble hitpoints from the wiki table; the attack levels and max hits are raised by the combat hook.
            setHitpoints(mob, NightmareZone.hardHitpoints(id));
            mob.addCommandTag(HARD_TAG);
        }
        aggro(mob);
        return mob;
    }

    /** Spawned bosses still alive. */
    private int bossesAlive() {
        spawnedBosses.removeIf(m -> m.isRemoved() || !m.isAlive());
        return spawnedBosses.size();
    }

    @Override
    protected void gameTick() {
        switch (dream.mode()) {
            case RUMBLE, CUSTOM_RUMBLE -> {
                if (ticks % 5 == 0 && bossesAlive() < NightmareZone.MAX_BOSSES && !pool.isEmpty()) {
                    spawnBoss(pool.get(random.nextInt(pool.size())));
                }
                powerUps();
            }
            case ENDURANCE -> {
                if (ticks % 5 == 0 && bossesAlive() == 0) {
                    if (next >= pool.size()) {
                        broadcast("You have defeated every boss in the dream!");
                        end(true);
                        return;
                    }
                    String boss = pool.get(next++);
                    broadcast("Wave " + next + " of " + pool.size() + ": " + bossName(boss) + ".");
                    spawnBoss(boss);
                }
            }
            case PRACTICE -> {
                if (!practiceSpawned && ticks >= 3) {
                    practiceSpawned = true;
                    spawnBoss(pool.get(0));
                } else if (practiceSpawned && ticks % 5 == 0 && bossesAlive() == 0) {
                    broadcast("You have defeated " + bossName(pool.get(0)) + ".");
                    end(true);
                    return;
                }
            }
        }
        for (ServerPlayerEntity p : online()) {
            overloadTick(p);
        }
    }

    // ------------------------------------------------------------------ power-ups

    private void powerUps() {
        if (spawned != null && ticks >= spawnedUntil) {
            spawned = null;
        }
        if (spawned == null && ticks >= nextPowerUp) {
            NightmareZone.PowerUp[] all = NightmareZone.PowerUp.values();
            spawned = all[random.nextInt(all.length)];
            spawnedAt = randomFloor(0, 0, 12);
            spawnedUntil = ticks + NightmareZone.POWER_UP_LIFETIME;
            nextPowerUp = ticks + NightmareZone.POWER_UP_INTERVAL;
            broadcast("A " + spawned.displayName().toLowerCase() + " power-up has appeared.");
        }
        if (spawned != null) {
            world.spawnParticles(new DustParticleEffect(colour(spawned), 1.5f), spawnedAt.getX() + 0.5, spawnedAt.getY() + 0.6,
                    spawnedAt.getZ() + 0.5, 6, 0.25, 0.4, 0.25, 0);
            for (ServerPlayerEntity p : online()) {
                if (p.getBlockPos().getSquaredDistance(spawnedAt) <= 2.5) {
                    activate(p, spawned);
                    spawned = null;
                    break;
                }
            }
        }
        for (ServerPlayerEntity p : online()) {
            powerTick(p);
        }
    }

    /** One game tick of a player's active power-up (public for the self-test, whose player isn't in the player list). */
    public void powerTick(ServerPlayerEntity p) {
        NightmareZone.PowerUp active = power.get(p.getUuid());
        if (active == null) {
            return;
        }
        if (ticks >= powerUntil.getOrDefault(p.getUuid(), 0L)) {
            power.remove(p.getUuid());
            Msg.red(p, "Your " + active.displayName().toLowerCase() + " power-up has worn off.");
            return;
        }
        if (active == NightmareZone.PowerUp.POWER_SURGE) {
            com.runecraft.content.systems.SpecialAttackService.restoreEnergy(p, NightmareZone.POWER_SURGE_PER_TICK);
        } else if (active == NightmareZone.PowerUp.ZAPPER && ticks % NightmareZone.ZAPPER_INTERVAL == 0) {
            for (LivingEntity boss : bossesNear(p, 6)) {
                zap(p, boss, random.nextInt(NightmareZone.ZAPPER_MAX_HIT + 1));
            }
        }
    }

    /**
     * One zap: the zapper can finish any boss except Count Draynor, who it only kills once the player has hit him (OSRS
     * Wiki). Returns the damage dealt (public for the self-test).
     */
    public int zap(ServerPlayerEntity p, LivingEntity boss, int roll) {
        int dmg = Math.max(0, roll);
        if (!NightmareZone.zapperMayKill(MinigameManager.creatureId(boss), hitByPlayer.contains(boss.getUuid()))) {
            dmg = Math.min(dmg, Math.max(0, (int) boss.getHealth() - 1));
        }
        if (dmg > 0) {
            CombatHandler.applyDamage(p, boss, dmg);
        }
        return dmg;
    }

    private static Vector3f colour(NightmareZone.PowerUp up) {
        return switch (up) {
            case RECURRENT_DAMAGE -> new Vector3f(0.9f, 0.15f, 0.15f);
            case ZAPPER -> new Vector3f(0.6f, 0.2f, 0.9f);
            case POWER_SURGE -> new Vector3f(1.0f, 0.9f, 0.2f);
            case ULTIMATE_FORCE -> new Vector3f(1.0f, 1.0f, 1.0f);
        };
    }

    private List<LivingEntity> bossesNear(ServerPlayerEntity p, double range) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : bosses()) {
            if (e.squaredDistanceTo(p) <= range * range) {
                out.add(e);
            }
        }
        return out;
    }

    /** Activates a power-up for a player (public for the self-test). */
    public void activate(ServerPlayerEntity p, NightmareZone.PowerUp up) {
        world.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 0.8f);
        if (up == NightmareZone.PowerUp.ULTIMATE_FORCE) {
            Msg.game(p, "You activate ultimate force! Every boss in the dream is destroyed.");
            forcing = true;
            try {
                for (LivingEntity boss : bossesNear(p, 100)) {
                    CombatHandler.applyDamage(p, boss, (int) Math.ceil(boss.getHealth()));
                }
            } finally {
                forcing = false;
            }
            return;
        }
        power.put(p.getUuid(), up);
        powerUntil.put(p.getUuid(), ticks + up.ticks());
        if (up == NightmareZone.PowerUp.POWER_SURGE) {
            com.runecraft.content.systems.SpecialAttackService.restoreEnergy(p, 100);
        }
        Msg.game(p, "You activate the " + up.displayName().toLowerCase() + " power-up.");
    }

    /** The power-up a player has active, or null. */
    public NightmareZone.PowerUp activePowerUp(ServerPlayerEntity p) {
        return power.get(p.getUuid());
    }

    /** The bosses alive in the dream (public for the self-test). */
    public List<LivingEntity> bosses() {
        bossesAlive();
        return new ArrayList<>(spawnedBosses);
    }

    /** Records that a player hit a boss (even for 0), which lets the zapper finish Count Draynor. */
    public void noteHit(LivingEntity boss) {
        hitByPlayer.add(boss.getUuid());
    }

    @Override
    protected void onPlayerHit(ServerPlayerEntity player, LivingEntity target, int dealt) {
        noteHit(target);
        if (dealt > 0 && power.get(player.getUuid()) == NightmareZone.PowerUp.RECURRENT_DAMAGE && target.isAlive()) {
            // The extra hit gives no experience (applyDamage awards none).
            CombatHandler.applyDamage(player, target, NightmareZone.recurrent(dealt));
        }
    }

    /** Hard dreams: the bosses attack with their hard attack levels and max hits (registered by the minigames module). */
    public static void register() {
        CombatHooks.addNpcVersusPlayer(new CombatHooks.NpcVersusPlayer() {
            @Override
            public double accuracy(LivingEntity mob, ServerPlayerEntity target, AttackType type) {
                if (!mob.getCommandTags().contains(HARD_TAG)) {
                    return 1.0;
                }
                return NightmareZone.hardAccuracy(MinigameManager.creatureId(mob), type != AttackType.RANGED && type != AttackType.MAGIC,
                        type == AttackType.RANGED);
            }

            @Override
            public int maxHitBonus(LivingEntity mob, ServerPlayerEntity target, AttackType type) {
                if (!mob.getCommandTags().contains(HARD_TAG) || type == AttackType.RANGED || type == AttackType.MAGIC) {
                    return 0;
                }
                return NightmareZone.hardMaxHitBonus(MinigameManager.creatureId(mob));
            }
        });
    }

    // ------------------------------------------------------------------ potions

    /** Drinks a Nightmare Zone potion (only inside a dream). Returns whether the click was handled. */
    public static boolean drink(ServerPlayerEntity player, ItemStack stack, Hand hand, String id) {
        if (!NightmareZone.POTIONS.contains(id)) {
            return false;
        }
        if (!(MinigameManager.session(player) instanceof NightmareZoneSession nmz)) {
            Msg.game(player, "You can only drink this potion in the Nightmare Zone.");
            return true;
        }
        if (nmz.drink(player, id)) {
            int used = stack.getDamage() + 1;
            int left = 4 - used;
            player.getItemCooldownManager().set(stack.getItem(), 3 * com.runecraft.action.ActionManager.MC_TICKS_PER_GAME_TICK);
            player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 0.5f, 1.0f);
            if (left <= 0) {
                // The empty Nightmare Zone vial is discarded (OSRS: no empty vials are needed or kept).
                player.setStackInHand(hand, ItemStack.EMPTY);
            } else {
                stack.setDamage(used);
            }
            ServerNetworking.sync(player);
        }
        return true;
    }

    /** Applies one dose; false if it can't be drunk now. Public for the self-test. */
    public boolean drink(ServerPlayerEntity p, String id) {
        SkillSet skills = Players.data(p).skills();
        switch (id) {
            case NightmareZone.OVERLOAD -> {
                if (overload.containsKey(p.getUuid())) {
                    Msg.game(p, "You should wait until your overload wears off before drinking another.");
                    return false;
                }
                if (p.getHealth() <= NightmareZone.OVERLOAD_HITS * NightmareZone.OVERLOAD_HIT) {
                    Msg.game(p, "You need more than 50 Hitpoints to survive the power of an overload.");
                    return false;
                }
                overload.put(p.getUuid(), ticks);
                overloadBoost(skills);
                Msg.game(p, "You drink some of your overload potion.");
            }
            case NightmareZone.ABSORPTION -> {
                int now = NightmareZone.absorb(absorption.getOrDefault(p.getUuid(), 0));
                absorption.put(p.getUuid(), now);
                Msg.game(p, "You drink some of your absorption potion. Absorption points: " + now + ".");
            }
            case NightmareZone.SUPER_RANGING -> {
                boost(skills, Skill.RANGED);
                Msg.game(p, "You drink some of your super ranging potion.");
            }
            case NightmareZone.SUPER_MAGIC -> {
                boost(skills, Skill.MAGIC);
                Msg.game(p, "You drink some of your super magic potion.");
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private static void boost(SkillSet skills, Skill skill) {
        int amount = NightmareZone.boost(skills.level(skill));
        if (skills.boost(skill) < amount) {
            skills.setBoost(skill, amount);
        }
    }

    private static void overloadBoost(SkillSet skills) {
        for (Skill s : OVERLOAD_SKILLS) {
            boost(skills, s);
        }
    }

    private void overloadTick(ServerPlayerEntity p) {
        Long start = overload.get(p.getUuid());
        if (start == null) {
            return;
        }
        long age = ticks - start;
        // Five hits of 10 over the first few seconds (every second tick); self-inflicted, so never absorbed.
        if (age < 2L * NightmareZone.OVERLOAD_HITS && age % 2 == 0) {
            hurt(p, Math.min(NightmareZone.OVERLOAD_HIT, Math.max(0, (int) p.getHealth() - 1)));
        }
        if (age >= NightmareZone.OVERLOAD_TICKS) {
            endOverload(p, true);
        } else if (age > 0 && age % NightmareZone.OVERLOAD_REAPPLY_TICKS == 0) {
            overloadBoost(Players.data(p).skills());
            ServerNetworking.sync(p);
        }
    }

    private void endOverload(ServerPlayerEntity p, boolean heal) {
        if (overload.remove(p.getUuid()) == null) {
            return;
        }
        SkillSet skills = Players.data(p).skills();
        for (Skill s : OVERLOAD_SKILLS) {
            if (skills.boost(s) > 0) {
                skills.setBoost(s, 0);
            }
        }
        if (heal) {
            p.heal(NightmareZone.OVERLOAD_HEAL);
            Msg.red(p, "The effects of overload have worn off, and you feel normal again.");
        }
        ServerNetworking.sync(p);
    }

    /** Absorption points a player has left (public for the self-test). */
    public int absorption(ServerPlayerEntity p) {
        return absorption.getOrDefault(p.getUuid(), 0);
    }

    /** Whether a player's overload is active (public for the self-test). */
    public boolean overloaded(ServerPlayerEntity p) {
        return overload.containsKey(p.getUuid());
    }

    /** Absorption soaks up damage from monsters (registered with CombatHooks by the minigames module). */
    public static int absorb(ServerPlayerEntity p, LivingEntity attacker, int damage) {
        if (damage <= 0 || attacker == null || attacker == p || !(MinigameManager.session(p) instanceof NightmareZoneSession nmz)) {
            return damage;
        }
        int points = nmz.absorption.getOrDefault(p.getUuid(), 0);
        if (points <= 0) {
            return damage;
        }
        int soaked = Math.min(points, damage);
        nmz.absorption.put(p.getUuid(), points - soaked);
        return damage - soaked;
    }

    // ------------------------------------------------------------------ points and leaving

    /** Points a kill of this boss pays in this dream (public for the self-test). */
    public int pointsFor(String creatureId) {
        if (forcing || dream.mode() == NightmareZone.Mode.PRACTICE) {
            return 0;
        }
        return NightmareZone.points(creatureId, hard, (int) pool.stream().distinct().count());
    }

    @Override
    protected void onKill(ServerPlayerEntity player, LivingEntity entity, String creatureId) {
        if (!NightmareZone.isBoss(creatureId)) {
            return;
        }
        kills++;
        int pts = pointsFor(creatureId);
        if (pts <= 0) {
            return;
        }
        earned += pts;
        addPoints(player, MinigameCurrencies.NIGHTMARE_ZONE, pts);
        Msg.action(player, "+" + String.format("%,d", pts) + " Nightmare Zone points");
    }

    @Override
    protected void onLeave(ServerPlayerEntity player) {
        endOverload(player, false);
        absorption.remove(player.getUuid());
        power.remove(player.getUuid());
        int removed = 0;
        for (String potion : NightmareZone.POTIONS) {
            removed += Inv.remove(player, potion, 10_000);
        }
        if (removed > 0) {
            Msg.game(player, "Your Nightmare Zone potions fade away as you wake.");
        }
        int total = MinigameData.of(player).points(player.getUuid(), MinigameCurrencies.key(MinigameCurrencies.NIGHTMARE_ZONE));
        Msg.game(player, "You wake up. This dream: " + kills + " bosses for " + String.format("%,d", earned)
                + " points. You have " + String.format("%,d", total) + " Nightmare Zone points.");
    }

    @Override
    protected List<String> hud(ServerPlayerEntity player) {
        NightmareZone.PowerUp up = power.get(player.getUuid());
        String progress = dream.mode() == NightmareZone.Mode.ENDURANCE ? "Wave: " + next + " / " + pool.size()
                : "Bosses in the arena: " + bossesAlive();
        return List.of("Nightmare Zone - " + (hard ? "hard " : "") + dream.mode().displayName.toLowerCase(), "Bosses defeated: " + kills,
                "Points this dream: " + String.format("%,d", earned), progress,
                "Absorption: " + absorption(player) + (overloaded(player) ? "  Overload active" : ""),
                up == null ? "" : "Power-up: " + up.displayName());
    }
}
