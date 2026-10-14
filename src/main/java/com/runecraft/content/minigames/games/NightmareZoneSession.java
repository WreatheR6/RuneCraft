package com.runecraft.content.minigames.games;

import com.runecraft.combat.CombatHandler;
import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.network.ServerNetworking;
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
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Nightmare Zone rumble: Dominic Onion's dream pits the player against the quest bosses, up to four
 * at once; each kill pays Nightmare Zone points (the wiki's per-boss normal or hard value) and another
 * boss takes its place until the player dies or leaves. A dream costs 2,000 coins (hard 6,000) and
 * needs five of the boss quests. Hard bosses have the wiki's hard hitpoints. Power-ups appear in the
 * arena (recurrent damage, zapper, power surge, ultimate force), and the potions from Dom Onion's
 * Reward Shop (overload, absorption, super ranging, super magic) only work inside a dream; they are
 * taken away on waking up. Points buy resources and imbues at Dom Onion's Reward Shop. Death is safe.
 */
public class NightmareZoneSession extends MinigameSession {
    private static final Skill[] OVERLOAD_SKILLS = {Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC};
    private final boolean hard;
    private int kills;
    private int earned;
    /** Absorption points per player. */
    private final Map<UUID, Integer> absorption = new HashMap<>();
    /** Game tick each player's overload started (absent when none is active). */
    private final Map<UUID, Long> overload = new HashMap<>();
    /** Active power-up per player and the tick it wears off. */
    private final Map<UUID, NightmareZone.PowerUp> power = new HashMap<>();
    private final Map<UUID, Long> powerUntil = new HashMap<>();
    /** The power-up waiting in the arena (null when none), where, and the tick it vanishes. */
    private NightmareZone.PowerUp spawned;
    private BlockPos spawnedAt;
    private long spawnedUntil;
    private long nextPowerUp = NightmareZone.POWER_UP_INTERVAL;
    /** Ultimate force kills pay no points. */
    private boolean forcing;

    public NightmareZoneSession(int id, int slot, ServerWorld world, String variant) {
        super(Minigame.NIGHTMARE_ZONE, id, slot, world, variant);
        this.hard = variant.contains("hard");
    }

    /** Entry check: five boss quests and the dream's fee in coins. */
    public static String requirement(ServerPlayerEntity player, String variant) {
        if (!NightmareZone.canEnter(q -> ContentRegistry.quest(q) != null && QuestApi.isComplete(player, q))) {
            return "Dominic Onion: Your brain is insufficiently trained for my dreams. Complete at least "
                    + NightmareZone.QUESTS_NEEDED + " quests with bosses in them first.";
        }
        int cost = variant.contains("hard") ? NightmareZone.HARD_RUMBLE_COST : NightmareZone.RUMBLE_COST;
        if (!Inv.has(player, "coins", cost)) {
            return "Dominic Onion: A " + (variant.contains("hard") ? "hard " : "") + "rumble costs " + String.format("%,d", cost) + " coins.";
        }
        return null;
    }

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

    @Override
    protected void start() {
        int cost = hard ? NightmareZone.HARD_RUMBLE_COST : NightmareZone.RUMBLE_COST;
        for (ServerPlayerEntity p : online()) {
            Inv.remove(p, "coins", cost);
            Msg.game(p, "You drink the dream potion and drift into a " + (hard ? "hard " : "") + "rumble...");
        }
    }

    private void spawnBoss() {
        Object[] b = NightmareZone.BOSSES[random.nextInt(NightmareZone.BOSSES.length)];
        MobEntity mob = spawn((String) b[0], randomFloor(0, -6, 10));
        if (mob == null) {
            return;
        }
        if (hard) {
            // Hard rumble hitpoints from the wiki table; the raised offensive levels are not modelled. // approx
            setHitpoints(mob, NightmareZone.hardHitpoints((String) b[0]));
        }
        aggro(mob);
    }

    @Override
    protected void gameTick() {
        if (ticks % 5 == 0 && mobsAlive() < NightmareZone.MAX_BOSSES) {
            spawnBoss();
        }
        powerUps();
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
            NightmareZone.PowerUp active = power.get(p.getUuid());
            if (active == null) {
                continue;
            }
            if (ticks >= powerUntil.getOrDefault(p.getUuid(), 0L)) {
                power.remove(p.getUuid());
                Msg.red(p, "Your " + active.displayName().toLowerCase() + " power-up has worn off.");
                continue;
            }
            if (active == NightmareZone.PowerUp.POWER_SURGE) {
                com.runecraft.content.systems.SpecialAttackService.restoreEnergy(p, NightmareZone.POWER_SURGE_PER_TICK);
            } else if (active == NightmareZone.PowerUp.ZAPPER && ticks % NightmareZone.ZAPPER_INTERVAL == 0) {
                for (LivingEntity boss : bossesNear(p, 6)) {
                    // The zapper never lands the killing blow on a boss the player hasn't hit (Count Draynor rule, approx. for all).
                    int dmg = Math.min(random.nextInt(NightmareZone.ZAPPER_MAX_HIT + 1), Math.max(0, (int) boss.getHealth() - 1));
                    CombatHandler.applyDamage(p, boss, dmg);
                }
            }
        }
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
        for (Object[] b : NightmareZone.BOSSES) {
            for (LivingEntity e : living((String) b[0])) {
                if (e.squaredDistanceTo(p) <= range * range) {
                    out.add(e);
                }
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

    @Override
    protected void onPlayerHit(ServerPlayerEntity player, LivingEntity target, int dealt) {
        if (dealt > 0 && power.get(player.getUuid()) == NightmareZone.PowerUp.RECURRENT_DAMAGE && target.isAlive()) {
            // The extra hit gives no experience (applyDamage awards none).
            CombatHandler.applyDamage(player, target, NightmareZone.recurrent(dealt));
        }
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
                // approx: the empty Nightmare Zone vial is discarded.
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

    @Override
    protected void onKill(ServerPlayerEntity player, LivingEntity entity, String creatureId) {
        int pts = forcing ? 0 : NightmareZone.points(creatureId, hard);
        if (pts <= 0) {
            return;
        }
        kills++;
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
        return List.of("Nightmare Zone - " + (hard ? "hard" : "normal") + " rumble", "Bosses defeated: " + kills,
                "Points this dream: " + String.format("%,d", earned), "Bosses in the arena: " + mobsAlive(),
                "Absorption: " + absorption(player) + (overloaded(player) ? "  Overload active" : ""),
                up == null ? "" : "Power-up: " + up.displayName());
    }
}
