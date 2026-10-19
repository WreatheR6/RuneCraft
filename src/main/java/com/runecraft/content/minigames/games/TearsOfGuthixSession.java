package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameRules.TearsOfGuthix;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestApi;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

/**
 * Tears of Guthix in the Chasm of Tears: for one game tick per quest point, collect blue tears from
 * the nine weeping walls (three blue and three green streams move every 16 ticks; green tears spill
 * the bowl). When time is up the tears are drunk for min(60, 10 + floor(xp / 27) / 10) XP each in
 * the eligible skill with the least experience. Playable once a week, after a quest point or
 * 100,000 XP since the last visit, once the Tears of Guthix quest is complete (OSRS Wiki).
 */
public class TearsOfGuthixSession extends MinigameSession {
    private static final int WALLS = 9;
    private static final long DAY_MILLIS = 86_400_000L;
    /** Stream on each wall: 0 none, 1 blue, 2 green. */
    private final int[] stream = new int[WALLS];
    private int tears;
    private int limit;

    public TearsOfGuthixSession(int id, int slot, ServerLevel world, String variant) {
        super(Minigame.TEARS_OF_GUTHIX, id, slot, world, variant);
    }

    private static boolean questDone(ServerPlayer player, String quest) {
        return ContentRegistry.quest(quest) != null && QuestApi.isComplete(player, quest);
    }

    private static int today() {
        return (int) (System.currentTimeMillis() / DAY_MILLIS);
    }

    /** Entry check: the quest, a week since the last visit and a quest point or 100,000 XP gained since. */
    public static String requirement(ServerPlayer player) {
        if (!questDone(player, "tears_of_guthix")) {
            return "Juna: You must complete the Tears of Guthix quest before you can collect the tears.";
        }
        MinigameData d = MinigameData.of(player);
        if (d.stat(player.getUUID(), "tog_visits") == 0) {
            return null;
        }
        int days = today() - d.stat(player.getUUID(), "tog_day");
        if (days < TearsOfGuthix.DAYS_BETWEEN) {
            return "Juna: Come back in " + (TearsOfGuthix.DAYS_BETWEEN - days) + " day" + (TearsOfGuthix.DAYS_BETWEEN - days > 1 ? "s" : "")
                    + ", adventurer.";
        }
        var data = Players.data(player);
        boolean qp = data.questPoints() > d.stat(player.getUUID(), "tog_qp");
        boolean xp = data.skills().totalXp() / 1000 - d.stat(player.getUUID(), "tog_kxp") >= TearsOfGuthix.XP_SINCE_LAST / 1000;
        if (!qp && !xp) {
            return "Juna: Gain a quest point or 100,000 experience before you tell me another story.";
        }
        return null;
    }

    @Override
    protected int halfX() {
        return 12;
    }

    @Override
    protected int halfZ() {
        return 8;
    }

    @Override
    protected void build() {
        room(Blocks.DEEPSLATE_TILES, Blocks.DEEPSLATE_BRICKS, Blocks.PRISMARINE);
        for (int i = 0; i < WALLS; i++) {
            place(wallX(i), -6, "tog_weeping_wall");
        }
        place(0, 7, "minigame_exit");
    }

    private static int wallX(int i) {
        return -8 + i * 2;
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayer player, int index) {
        return rel(0, 1, 2);
    }

    @Override
    protected void start() {
        for (ServerPlayer p : online()) {
            limit = TearsOfGuthix.ticks(Players.data(p).questPoints());
            MinigameData d = MinigameData.of(p);
            d.incrementStat(p.getUUID(), "tog_visits");
            d.setStat(p.getUUID(), "tog_day", today());
            d.setStat(p.getUUID(), "tog_qp", Players.data(p).questPoints());
            d.setStat(p.getUUID(), "tog_kxp", (int) Math.min(Integer.MAX_VALUE, Players.data(p).skills().totalXp() / 1000));
            Msg.game(p, "Juna lets you into the chasm with a stone bowl. Collect the blue tears, avoid the green! (" + limit + " ticks)");
        }
        moveStreams();
    }

    private void moveStreams() {
        List<Integer> walls = new ArrayList<>();
        for (int i = 0; i < WALLS; i++) {
            walls.add(i);
        }
        Collections.shuffle(walls, random);
        java.util.Arrays.fill(stream, 0);
        for (int i = 0; i < 6; i++) {
            stream[walls.get(i)] = i < 3 ? 1 : 2;
        }
    }

    @Override
    protected boolean onBlock(ServerPlayer player, String blockId, BlockPos pos) {
        if (!blockId.equals("tog_weeping_wall")) {
            return false;
        }
        int wall = (pos.getX() - origin.getX() + 8) / 2;
        if (wall < 0 || wall >= WALLS) {
            return true;
        }
        repeat(player, p -> {
            if (p.blockPosition().distSqr(pos) > 9 || stream[wall] == 0) {
                return false;
            }
            if (stream[wall] == 1) {
                tears++;
            } else {
                tears = Math.max(0, tears - 1);
            }
            return true;
        });
        Msg.action(player, stream[wall] == 1 ? "You collect the blue tears." : stream[wall] == 2 ? "Green tears spill into your bowl!"
                : "This wall is dry right now.");
        return true;
    }

    @Override
    protected void gameTick() {
        if (ticks % TearsOfGuthix.STREAM_TICKS == 0) {
            moveStreams();
        }
        if (ticks % 4 == 0) {
            for (int i = 0; i < WALLS; i++) {
                if (stream[i] != 0) {
                    BlockPos at = rel(wallX(i), 2, -6);
                    world.sendParticles(stream[i] == 1 ? ParticleTypes.FALLING_WATER : ParticleTypes.FALLING_SPORE_BLOSSOM,
                            at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 6, 0.2, 0.4, 0.2, 0);
                }
            }
        }
        if (ticks >= limit) {
            end(true);
        }
    }

    @Override
    protected void onEnd(boolean success) {
        for (ServerPlayer p : online()) {
            SkillSet skills = Players.data(p).skills();
            Skill skill = TearsOfGuthix.skill(skills::xp, s -> switch (s) {
                case RUNECRAFT -> questDone(p, "RUNE_MYSTERIES");
                case HERBLORE -> questDone(p, "DRUIDIC_RITUAL");
                case SAILING -> skills.xp(s) > 0; // approx: needs Pandemonium in OSRS
                default -> true;
            });
            double xp = skill == null ? 0 : TearsOfGuthix.xp(tears, skills.xp(skill));
            if (xp > 0) {
                xp(p, skill, xp);
            }
            Msg.game(p, "You drink the " + tears + " tears you collected" + (xp > 0 ? " and gain " + (int) xp + " " + skill.displayName()
                    + " experience." : "."));
        }
    }

    @Override
    protected List<String> hud(ServerPlayer player) {
        return List.of("Tears of Guthix", "Tears: " + tears, "Time left: " + time(Math.max(0, limit - ticks)));
    }
}
