package com.runecraft.content.minigames.games;

import com.runecraft.action.ActionManager;
import com.runecraft.combat.CombatHandler;
import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameItemUse;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameRules.Wintertodt;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Wintertodt: chop bruma roots, fletch them into kindling, and feed the four braziers to drain the
 * Wintertodt's energy. Braziers break (fix with a hammer) and get snuffed out; pyromancers fall and
 * need a rejuvenation potion (bruma herb + crate) before their brazier can be relit. The cold and
 * the Wintertodt's snowfall attacks hurt more with less warm clothing. 500 points earns a supply
 * crate (one roll per 500 points) and bonus Firemaking experience.
 */
public class WintertodtSession extends MinigameSession {
    private static final int[][] BRAZIERS = {{-8, -8}, {8, -8}, {-8, 8}, {8, 8}};
    private static final int[][] ROOTS = {{-15, -15}, {15, -15}, {-15, 15}, {15, 15}};
    private enum State { UNLIT, LIT, BROKEN }

    private final State[] braziers = {State.UNLIT, State.UNLIT, State.UNLIT, State.UNLIT};
    private final boolean[] pyromancerDown = new boolean[4];
    private final Map<UUID, Integer> points = new HashMap<>();
    private final List<Object[]> snowfalls = new ArrayList<>();
    private int energy;
    private int maxEnergy;

    public WintertodtSession(int id, int slot, ServerWorld world, String variant) {
        super(Minigame.WINTERTODT, id, slot, world, variant);
    }

    @Override
    protected int height() {
        return 14;
    }

    @Override
    protected void build() {
        room(Blocks.SNOW_BLOCK, Blocks.PACKED_ICE, Blocks.BLUE_ICE);
        for (int i = 0; i < 4; i++) {
            place(BRAZIERS[i][0], BRAZIERS[i][1], "wintertodt_brazier");
            place(ROOTS[i][0], ROOTS[i][1], "bruma_roots");
        }
        place(-3, 18, "wintertodt_crate");
        place(3, 18, "sprouting_roots");
        place(0, 21, "minigame_exit");
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                set(x, 0, z, Blocks.BLUE_ICE.getDefaultState());
            }
        }
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayerEntity player, int index) {
        return rel(index % 5 - 2, 1, 19);
    }

    @Override
    protected void start() {
        maxEnergy = Wintertodt.energy(playerCount());
        energy = maxEnergy;
        MobEntity todt = spawn("wintertodt", rel(0, 1, 0));
        if (todt != null) {
            todt.setAiDisabled(true);
        }
        broadcastRed("The Wintertodt awakens! Feed the braziers to subdue it.");
    }

    private int index(BlockPos pos, int[][] table) {
        BlockPos r = pos.subtract(origin);
        int best = 0;
        for (int i = 1; i < table.length; i++) {
            if (Math.abs(r.getX() - table[i][0]) + Math.abs(r.getZ() - table[i][1])
                    < Math.abs(r.getX() - table[best][0]) + Math.abs(r.getZ() - table[best][1])) {
                best = i;
            }
        }
        return best;
    }

    private void addPoints(ServerPlayerEntity p, int amount) {
        points.merge(p.getUuid(), amount, Integer::sum);
    }

    private int lit() {
        int n = 0;
        for (State s : braziers) {
            if (s == State.LIT) {
                n++;
            }
        }
        return n;
    }

    private void damageTodt(int amount) {
        energy = Math.max(0, energy - amount);
    }

    @Override
    protected boolean onBlock(ServerPlayerEntity player, String blockId, BlockPos pos) {
        switch (blockId) {
            case "bruma_roots" -> {
                if (!Inv.has(player, "bronze_axe", 1) && !hasAnyAxe(player)) {
                    Msg.game(player, "You need an axe to chop the bruma roots. There are some in the supply crate.");
                    return true;
                }
                Msg.action(player, "You swing your axe at the roots.");
                ActionManager.start(player, (p, t) -> {
                    if (!Inv.hasRoomFor(p, "bruma_root")) {
                        Msg.game(p, "Your inventory is too full to hold any more roots.");
                        return false;
                    }
                    if (random.nextInt(100) < 35 + level(p, Skill.WOODCUTTING) / 2) {
                        Inv.give(p, "bruma_root", 1);
                        xp(p, Skill.WOODCUTTING, Wintertodt.chopXp(level(p, Skill.WOODCUTTING)));
                        Msg.action(p, "You get a bruma root.");
                    }
                    return true;
                });
                return true;
            }
            case "sprouting_roots" -> {
                if (Inv.hasRoomFor(player, "bruma_herb")) {
                    Inv.give(player, "bruma_herb", 1);
                    xp(player, Skill.FARMING, 3);
                    Msg.game(player, "You pick a bruma herb.");
                }
                return true;
            }
            case "wintertodt_crate" -> {
                crate(player);
                return true;
            }
            case "wintertodt_brazier" -> {
                brazier(player, index(pos, BRAZIERS));
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private static boolean hasAnyAxe(ServerPlayerEntity p) {
        for (String tier : new String[]{"bronze", "iron", "steel", "black", "mithril", "adamant", "rune", "dragon"}) {
            if (Inv.has(p, tier + "_axe", 1)) {
                return true;
            }
        }
        return false;
    }

    private void crate(ServerPlayerEntity player) {
        int herbs = Inv.count(player, "bruma_herb");
        if (herbs > 0) {
            Inv.remove(player, "bruma_herb", herbs);
            Inv.give(player, "rejuvenation_potion", herbs);
            xp(player, Skill.HERBLORE, 14 * herbs);
            Msg.game(player, "You mix the bruma herbs into rejuvenation potions.");
            return;
        }
        int roots = Inv.count(player, "bruma_root");
        if (roots > 0 && Inv.has(player, "knife", 1)) {
            Msg.action(player, "You begin fletching the bruma roots into kindling.");
            ActionManager.start(player, (p, t) -> {
                if (!Inv.has(p, "bruma_root", 1)) {
                    return false;
                }
                Inv.remove(p, "bruma_root", 1);
                Inv.give(p, "bruma_kindling", 1);
                xp(p, Skill.FLETCHING, Wintertodt.fletchXp(level(p, Skill.FLETCHING)));
                return true;
            });
            return;
        }
        boolean gave = false;
        for (String tool : new String[]{"knife", "hammer", "tinderbox"}) {
            if (!Inv.has(player, tool, 1)) {
                Inv.give(player, tool, 1);
                gave = true;
            }
        }
        if (!hasAnyAxe(player)) {
            Inv.give(player, "bronze_axe", 1);
            gave = true;
        }
        Msg.game(player, gave ? "You take some tools from the crate." : "The crate has knives, hammers, tinderboxes and axes. "
                + "Use it with bruma roots (and a knife) to fletch, or with bruma herbs to make potions.");
    }

    private void brazier(ServerPlayerEntity player, int i) {
        switch (braziers[i]) {
            case BROKEN -> {
                if (!Inv.has(player, "hammer", 1)) {
                    Msg.game(player, "You need a hammer to fix the brazier.");
                    return;
                }
                braziers[i] = State.UNLIT;
                addPoints(player, Wintertodt.POINTS_FIX);
                xp(player, Skill.CONSTRUCTION, Wintertodt.fixXp(level(player, Skill.CONSTRUCTION)));
                Msg.game(player, "You fix the brazier.");
            }
            case UNLIT -> {
                if (pyromancerDown[i]) {
                    if (Inv.remove(player, "rejuvenation_potion", 1) > 0) {
                        pyromancerDown[i] = false;
                        addPoints(player, Wintertodt.POINTS_HEAL_PYROMANCER);
                        Msg.game(player, "You help the pyromancer back to their feet.");
                    } else {
                        Msg.game(player, "The pyromancer is down! Heal them with a rejuvenation potion before relighting.");
                    }
                    return;
                }
                if (!Inv.has(player, "tinderbox", 1) && !"bruma_torch".equals(Inv.idOf(player.getMainHandStack()))) {
                    Msg.game(player, "You need a tinderbox to light the brazier.");
                    return;
                }
                braziers[i] = State.LIT;
                addPoints(player, Wintertodt.POINTS_LIGHT);
                xp(player, Skill.FIREMAKING, Wintertodt.lightXp(level(player, Skill.FIREMAKING)));
                world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.BLOCKS, 1f, 1f);
                Msg.game(player, "You light the brazier.");
            }
            case LIT -> {
                Msg.action(player, "You start feeding the brazier.");
                ActionManager.start(player, (p, t) -> {
                    if (braziers[i] != State.LIT || isEnded()) {
                        return false;
                    }
                    if (Inv.remove(p, "bruma_kindling", 1) > 0) {
                        addPoints(p, Wintertodt.POINTS_KINDLING);
                        damageTodt(Wintertodt.POINTS_KINDLING);
                        xp(p, Skill.FIREMAKING, Wintertodt.burnKindlingXp(level(p, Skill.FIREMAKING)));
                        return true;
                    }
                    if (Inv.remove(p, "bruma_root", 1) > 0) {
                        addPoints(p, Wintertodt.POINTS_ROOT);
                        damageTodt(Wintertodt.POINTS_ROOT);
                        xp(p, Skill.FIREMAKING, Wintertodt.burnRootXp(level(p, Skill.FIREMAKING)));
                        return true;
                    }
                    Msg.game(p, "You have run out of bruma roots.");
                    return false;
                });
            }
        }
    }

    private int warmth(ServerPlayerEntity p) {
        int warm = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (Inv.idOf(p.getEquippedStack(slot)).startsWith("pyromancer_")) {
                warm++;
            }
        }
        String off = Inv.idOf(p.getOffHandStack());
        if (off.equals("fire_cape") || off.equals("infernal_cape") || off.equals("obsidian_cape") || off.equals("tome_of_fire")) {
            warm++;
        }
        if (Inv.idOf(p.getMainHandStack()).equals("bruma_torch")) {
            warm++;
        }
        if (Inv.has(p, "warm_gloves", 1)) {
            warm++;
        }
        return Math.min(4, warm);
    }

    @Override
    protected void gameTick() {
        if (energy <= 0) {
            broadcastRed("The Wintertodt is subdued!");
            end(true);
            return;
        }
        // Lit braziers slowly sap the Wintertodt.
        if (ticks % 2 == 0) {
            damageTodt(lit());
        }
        for (int i = 0; i < 4; i++) {
            BlockPos b = rel(BRAZIERS[i][0], 2, BRAZIERS[i][1]);
            if (braziers[i] == State.LIT) {
                world.spawnParticles(ParticleTypes.FLAME, b.getX() + 0.5, b.getY(), b.getZ() + 0.5, 4, 0.2, 0.3, 0.2, 0.02);
                int roll = random.nextInt(250);
                if (roll == 0) {
                    braziers[i] = State.BROKEN;
                    broadcastRed("A brazier shatters!");
                    for (ServerPlayerEntity p : online()) {
                        if (p.getBlockPos().getSquaredDistance(b) < 9) {
                            CombatHandler.applyDamage(null, p, 1 + random.nextInt(6));
                        }
                    }
                } else if (roll == 1) {
                    braziers[i] = State.UNLIT;
                    broadcast("The cold of the Wintertodt extinguishes a brazier.");
                } else if (roll == 2) {
                    braziers[i] = State.UNLIT;
                    pyromancerDown[i] = true;
                    broadcastRed("A pyromancer has fallen!");
                }
            } else if (braziers[i] == State.BROKEN) {
                world.spawnParticles(ParticleTypes.SMOKE, b.getX() + 0.5, b.getY(), b.getZ() + 0.5, 2, 0.2, 0.2, 0.2, 0.01);
            }
        }
        // The cold.
        if (ticks % 12 == 0) {
            for (ServerPlayerEntity p : online()) {
                int dmg = Wintertodt.coldDamage(level(p, Skill.HITPOINTS), warmth(p), lit());
                CombatHandler.applyDamage(null, p, dmg);
                com.runecraft.network.ServerNetworking.hitsplat(p, dmg, com.runecraft.network.Net.SPLAT_DAMAGE);
                if (random.nextInt(3) == 0) {
                    ActionManager.cancel(p);
                    Msg.action(p, "The cold of the Wintertodt seeps into your bones.");
                }
            }
        }
        // Snowfall: a telegraphed area attack on a random player.
        if (ticks % 25 == 0 && !online().isEmpty()) {
            List<ServerPlayerEntity> online = online();
            ServerPlayerEntity target = online.get(random.nextInt(online.size()));
            snowfalls.add(new Object[]{target.getPos(), ticks + 3});
        }
        snowfalls.removeIf(s -> {
            Vec3d at = (Vec3d) s[0];
            world.spawnParticles(ParticleTypes.SNOWFLAKE, at.x, at.y + 3, at.z, 20, 1.2, 0.5, 1.2, 0.01);
            if (ticks < (Long) s[1]) {
                return false;
            }
            for (ServerPlayerEntity p : online()) {
                if (p.getPos().squaredDistanceTo(at) < 4) {
                    int dmg = 3 + random.nextInt(8);
                    CombatHandler.applyDamage(null, p, dmg);
                    com.runecraft.network.ServerNetworking.hitsplat(p, dmg, com.runecraft.network.Net.SPLAT_DAMAGE);
                }
            }
            return true;
        });
    }

    @Override
    protected void onEnd(boolean success) {
        if (!success) {
            return;
        }
        for (ServerPlayerEntity p : online()) {
            int pts = points.getOrDefault(p.getUuid(), 0);
            int rolls = Wintertodt.rolls(pts);
            if (rolls > 0) {
                xp(p, Skill.FIREMAKING, Wintertodt.completionXp(level(p, Skill.FIREMAKING)));
                Inv.give(p, MinigameItemUse.container("supply_crate", rolls));
                int kc = MinigameData.of(p).incrementStat(p.getUuid(), "kc:" + game.id());
                Msg.game(p, "You have helped to subdue the Wintertodt! You earned " + pts + " points and a supply crate. "
                        + "Your subdued Wintertodt count is: " + kc + ".");
            } else {
                Msg.game(p, "You didn't earn enough points (" + pts + "/500) for a reward.");
            }
        }
    }

    @Override
    protected List<String> hud(ServerPlayerEntity player) {
        return List.of("Wintertodt", "Wintertodt's Energy: " + (maxEnergy == 0 ? 100 : energy * 100 / maxEnergy) + "%",
                "Points: " + points.getOrDefault(player.getUuid(), 0), "Braziers lit: " + lit() + " / 4",
                "Warmth: " + warmth(player) + " / 4");
    }
}
