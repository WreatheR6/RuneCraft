package com.runecraft.content.minigames.games;

import com.runecraft.combat.CombatHandler;
import com.runecraft.content.GameEvents;
import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameRules.BarbarianAssault;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;

/**
 * Barbarian Assault: ten waves of Penance fighters, rangers, runners and healers, then the
 * Penance Queen. Each player picks a role at its entrance: Attackers hit fighters and rangers
 * harder, Defenders stop runners (runners escaping costs the team), Collectors gather penance eggs
 * to load into the egg launcher (which blasts the Queen), Healers heal the team at the spring and
 * crush the penance healers. Clearing waves earns honour points in your role.
 */
public class BarbarianAssaultSession extends MinigameSession {
    private final Map<UUID, Integer> roles = new HashMap<>();
    private int wave;
    private long waveStart;
    private int nextWaveIn = 10;
    private int escaped;
    private boolean cleared = true;
    private Mob queen;

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
    protected void build() {
        room(Blocks.COARSE_DIRT, Blocks.SPRUCE_LOG, Blocks.MUD_BRICKS);
        for (int x = -4; x <= 4; x++) {
            set(x, 1, -19, Blocks.AIR.defaultBlockState());
            set(x, 0, -21, Blocks.BLACKSTONE.defaultBlockState());
        }
        place(-4, 16, "ba_egg_launcher");
        place(4, 16, "ba_healer_spring");
        place(0, 20, "minigame_exit");
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayer player, int index) {
        return rel(index % 5 - 2, 1, 17);
    }

    private int role(ServerPlayer p) {
        return roles.getOrDefault(p.getUUID(), 0);
    }

    private static String roleName(int r) {
        String n = BarbarianAssault.ROLES[r];
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    @Override
    protected void start() {
        for (ServerPlayer p : online()) {
            String v = MinigameManager.chosenVariant(p);
            int r = 0;
            for (int i = 0; i < BarbarianAssault.ROLES.length; i++) {
                if (v.contains(BarbarianAssault.ROLES[i])) {
                    r = i;
                }
            }
            roles.put(p.getUUID(), r);
            Msg.game(p, "Your role this game is: " + roleName(r) + ".");
        }
    }

    @Override
    protected void gameTick() {
        if (wave > 0) {
            // Runners escape into the cave if not stopped within a minute.
            if (ticks - waveStart > 100) {
                for (LivingEntity runner : living("penance_runner")) {
                    runner.discard();
                    escaped++;
                    broadcastRed("A penance runner has escaped!");
                }
            }
            for (LivingEntity runner : living("penance_runner")) {
                if (runner instanceof Mob m && m.getNavigation().isDone()) {
                    BlockPos to = randomFloor(0, -10, 10);
                    m.getNavigation().moveTo(to.getX(), to.getY(), to.getZ(), 1.2);
                }
            }
            if (queen != null && queen.isAlive()) {
                return;
            }
            if (mobsAlive() > 0) {
                return;
            }
            if (wave >= BarbarianAssault.WAVES) {
                return;
            }
            if (!cleared) {
                cleared = true;
                completeWave();
            }
        }
        if (--nextWaveIn > 0) {
            return;
        }
        nextWaveIn = 10;
        cleared = false;
        wave++;
        waveStart = ticks;
        broadcastRed("Wave " + wave + " begins!");
        int[] counts = BarbarianAssault.wave(wave);
        for (int type = 0; type < 4; type++) {
            for (int n = 0; n < counts[type]; n++) {
                Mob mob = spawn(BarbarianAssault.PENANCE[type], randomFloor(0, -17, 4));
                if (mob != null) {
                    setHitpoints(mob, BarbarianAssault.hitpoints(type, wave));
                    if (type != 2) {
                        aggro(mob);
                    }
                }
            }
        }
        if (wave == BarbarianAssault.WAVES) {
            queen = spawn("penance_queen", rel(0, 1, -16));
            if (queen != null) {
                aggro(queen);
            }
            broadcastRed("The Penance Queen emerges! Collectors, load eggs into the launcher!");
        }
    }

    private void completeWave() {
        for (ServerPlayer p : online()) {
            int r = role(p);
            int honour = giveHonour(p, r, BarbarianAssault.waveHonour(r, wave, escaped));
            Msg.game(p, "Wave " + wave + " complete! You earn " + honour + " " + roleName(r) + " honour points"
                    + (escaped > 0 ? " (" + escaped + " runner" + (escaped > 1 ? "s" : "") + " escaped)." : "."));
            // The round's progress (the next wave) unlocks low (wave 3) and medium (wave 6) gambles.
            if (waveReached(p) < wave + 1) {
                MinigameData.of(p).setStat(p.getUUID(), WAVE_STAT, wave + 1);
            }
            GameEvents.fireActivity(p, "ba_wave");
        }
        escaped = 0;
    }

    /** Adds honour points in a role, capped at 5,000; returns the amount added. */
    private int giveHonour(ServerPlayer p, int role, int amount) {
        String key = "ba_" + BarbarianAssault.ROLES[role];
        int add = BarbarianAssault.addHonour(MinigameData.of(p).points(p.getUUID(), key), amount);
        addPoints(p, "pts:" + key, add);
        return add;
    }

    @Override
    protected double damageMultiplier(ServerPlayer player, LivingEntity target, AttackType type) {
        String cid = MinigameManager.creatureId(target);
        return switch (role(player)) {
            // Attacker level: +1 to +5 bonus damage, approximated as +5% per level over 1. // approx
            case 0 -> cid.equals("penance_fighter") || cid.equals("penance_ranger")
                    ? 1.25 + 0.05 * (BarbarianAssault.attackerBonus(roleLevel(player, 0)) - 1) : 1.0;
            case 1 -> cid.equals("penance_runner") ? 2.0 : 1.0;
            case 3 -> cid.equals("penance_healer") ? 1.5 : 1.0;
            default -> 1.0;
        };
    }

    @Override
    protected void onKill(ServerPlayer player, LivingEntity entity, String creatureId) {
        if (creatureId.equals("penance_fighter") || creatureId.equals("penance_ranger")) {
            for (ServerPlayer p : online()) {
                if (role(p) == 2) {
                    // The collection bag holds 2 / 4 / 6 / 7 / 8 eggs by collector level.
                    int room = BarbarianAssault.collectorBag(roleLevel(p, 2)) - Inv.count(p, "penance_egg");
                    if (room > 0) {
                        Inv.give(p, "penance_egg", Math.min(room, 1 + random.nextInt(2)));
                    }
                }
            }
            if (online().stream().noneMatch(p -> role(p) == 2)) {
                Inv.give(player, "penance_egg", 1);
            }
        } else if (creatureId.equals("penance_queen")) {
            broadcastRed("The Penance Queen has been defeated!");
            end(true);
        }
    }

    @Override
    protected boolean onBlock(ServerPlayer player, String blockId, BlockPos pos) {
        if (blockId.equals("ba_egg_launcher")) {
            int eggs = Inv.remove(player, "penance_egg", 1000);
            if (eggs == 0) {
                Msg.game(player, "You need penance eggs to load the launcher.");
                return true;
            }
            if (queen != null && queen.isAlive()) {
                int dmg = eggs * BarbarianAssault.omegaEggDamage();
                CombatHandler.applyDamage(player, queen, dmg);
                world.sendParticles(ParticleTypes.EXPLOSION, queen.getX(), queen.getY() + 1, queen.getZ(), eggs, 1, 1, 1, 0);
                Msg.game(player, "You fire " + eggs + " omega egg" + (eggs > 1 ? "s" : "") + " at the Penance Queen!");
                if (!queen.isAlive()) {
                    broadcastRed("The Penance Queen has been defeated!");
                    end(true);
                }
            } else {
                addPoints(player, "pts:ba_collector", eggs / 2);
                Msg.game(player, "You load " + eggs + " eggs into the cannon for the attackers. (+" + eggs / 2 + " collector points)");
            }
            return true;
        }
        if (blockId.equals("ba_healer_spring")) {
            int healed = 0;
            for (ServerPlayer p : online()) {
                if (p.distanceToSqr(player) < 64 && p.getHealth() < p.getMaxHealth()) {
                    // Healer level: 10 / 15 / 20 / 25 / 35 hitpoints a heal.
                    p.heal(role(player) == 3 ? BarbarianAssault.healerHeal(roleLevel(player, 3)) : 4);
                    healed++;
                }
            }
            if (role(player) == 3 && healed > 1) {
                addPoints(player, "pts:ba_healer", 1);
            }
            Msg.game(player, "You draw healing water from the spring.");
            return true;
        }
        return false;
    }

    @Override
    protected void onEnd(boolean success) {
        if (!success) {
            return;
        }
        for (ServerPlayer p : online()) {
            int r = role(p);
            // Wave 10: 80 points in your role and 5 in each of the others.
            for (int other = 0; other < BarbarianAssault.ROLES.length; other++) {
                giveHonour(p, other, other == r ? BarbarianAssault.WAVE_10_ROLE : BarbarianAssault.WAVE_10_OTHER);
            }
            int kc = MinigameData.of(p).incrementStat(p.getUUID(), "kc:" + game.id());
            String queenKey = com.runecraft.osrs.content.minigames.MinigameCurrencies.key(
                    com.runecraft.osrs.content.minigames.MinigameCurrencies.BA_QUEEN);
            if (MinigameData.of(p).points(p.getUUID(), queenKey) < BarbarianAssault.MAX_QUEEN_KILLS) {
                addPoints(p, com.runecraft.osrs.content.minigames.MinigameCurrencies.BA_QUEEN, 1);
            }
            Msg.game(p, "Congratulations! You've defeated the Penance Queen. Queen kills: " + kc + ".");
            // The pet penance queen comes from high gambles (BaGambles), not from the kill itself (OSRS Wiki).
            // Finishing wave 10 resets the round, so low and medium gambles need new waves.
            MinigameData.of(p).setStat(p.getUUID(), WAVE_STAT, 0);
            Inv.remove(p, "penance_egg", 1000);
        }
    }

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
        MinigameData.of(p).incrementStat(p.getUUID(), "ba_gambles_" + tier.name().toLowerCase(java.util.Locale.ROOT));
        Msg.game(p, "Commander Connad hands you the prize of your " + tier.displayName().toLowerCase(java.util.Locale.ROOT) + ".");
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
        GameEvents.fireActivity(p, "ba_blackboard");
        if (BarbarianAssault.allMaxed(roleLevels(p))) {
            GameEvents.fireActivity(p, "ba_blackboard:level5");
        }
    }

    @Override
    protected List<String> hud(ServerPlayer player) {
        int r = role(player);
        int honour = MinigameData.of(player).points(player.getUUID(), "ba_" + BarbarianAssault.ROLES[r]);
        return List.of("Barbarian Assault", "Wave: " + Math.max(1, wave) + " / " + BarbarianAssault.WAVES,
                "Role: " + roleName(r) + " lv " + roleLevel(player, r) + " (" + honour + " honour)", "Penance left: " + mobsAlive(),
                queen != null && queen.isAlive() ? "Queen: " + (int) queen.getHealth() + " / " + BarbarianAssault.QUEEN_HP : "");
    }
}
