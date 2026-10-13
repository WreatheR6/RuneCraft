package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameCurrencies;
import com.runecraft.osrs.content.minigames.MinigameRules.NightmareZone;
import com.runecraft.quest.QuestApi;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Nightmare Zone rumble: Dominic Onion's dream pits the player against the quest bosses, up to four
 * at once; each kill pays Nightmare Zone points (more in a hard rumble) and another boss takes its
 * place until the player dies or leaves. A dream costs 2,000 coins (hard 6,000) and needs five of
 * the boss quests. Points buy resources and imbues at Dom Onion's Reward Shop. Death is safe.
 */
public class NightmareZoneSession extends MinigameSession {
    private final boolean hard;
    private int kills;
    private int earned;

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
            setHitpoints(mob, (int) Math.min(255, Math.round(mob.getMaxHealth() * 2))); // approx: hard bosses have about twice the hitpoints
        }
        aggro(mob);
    }

    @Override
    protected void gameTick() {
        if (ticks % 5 == 0 && mobsAlive() < NightmareZone.MAX_BOSSES) {
            spawnBoss();
        }
    }

    @Override
    protected void onKill(ServerPlayerEntity player, LivingEntity entity, String creatureId) {
        int pts = NightmareZone.points(creatureId, hard);
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
        int total = MinigameData.of(player).points(player.getUuid(), MinigameCurrencies.key(MinigameCurrencies.NIGHTMARE_ZONE));
        Msg.game(player, "You wake up. This dream: " + kills + " bosses for " + String.format("%,d", earned)
                + " points. You have " + String.format("%,d", total) + " Nightmare Zone points.");
    }

    @Override
    protected List<String> hud(ServerPlayerEntity player) {
        return List.of("Nightmare Zone - " + (hard ? "hard" : "normal") + " rumble", "Bosses defeated: " + kills,
                "Points this dream: " + String.format("%,d", earned), "Bosses in the arena: " + mobsAlive());
    }
}
