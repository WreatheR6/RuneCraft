package com.runecraft.content.minigames.games;

import com.runecraft.action.ActionManager;
import com.runecraft.combat.CombatHandler;
import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameItemUse;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameRules.Wintertodt;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Wintertodt (OSRS Wiki "Wintertodt", "Wintertodt/Strategies", after the 9 October 2024 update): chop bruma roots, fletch
 * them into kindling and feed the four braziers for points and experience. The pyromancers at the lit braziers drain the
 * Wintertodt's energy meter (1% every 14 ticks each; feeding doesn't speed it up; with every brazier out it regains 1% every
 * 35 ticks). The Wintertodt attacks the players' warmth meter (it replaced Hitpoints in the prison): the standard cold, a
 * 3x3 snowfall, and shattering braziers, more often early in the fight, while late in the fight it goes for the braziers
 * (shattered: fix with a hammer; snuffed out) and the pyromancers (14 hitpoints, two hits: heal them with a rejuvenation
 * potion). Warm clothing (four pieces) cuts the cold; food restores 35% warmth, a rejuvenation potion 30%; warmth
 * regenerates 8% a minute plus 1% per warm item; at 0% the player dies. The lobby by the doors is safe. 500 points earn the
 * reward rolls (2 at 500 points, +1 per further 500, max 28) and bonus Firemaking experience.
 */
public class WintertodtSession extends MinigameSession {
    private static final int[][] BRAZIERS = {{-8, -8}, {8, -8}, {-8, 8}, {8, 8}};
    private static final int[][] ROOTS = {{-15, -15}, {15, -15}, {-15, 15}, {15, 15}};
    /** The safe area by the doors (approx: blocks south of this row): none of the Wintertodt's attacks reach it. */
    private static final int SAFE_FROM_Z = 16;
    /** Game ticks between attempts at the braziers and pyromancers, and between snowfalls (approx: the wiki says "random"). */
    private static final int BRAZIER_ATTACK_TICKS = 8;
    private static final int SNOWFALL_TICKS = 25;
    /** Warning before a snowfall or a brazier attack lands. */
    private static final int WARNING_TICKS = 3;

    private enum State { UNLIT, LIT, BROKEN }

    private final State[] braziers = {State.UNLIT, State.UNLIT, State.UNLIT, State.UNLIT};
    private final int[] pyromancerHp = {Wintertodt.PYROMANCER_HP, Wintertodt.PYROMANCER_HP, Wintertodt.PYROMANCER_HP,
            Wintertodt.PYROMANCER_HP};
    /** Ticks until each pyromancer's next bolt at the Wintertodt. */
    private final int[] drainIn = new int[4];
    private int regainIn = Wintertodt.REGAIN_TICKS;
    private final Map<UUID, Integer> points = new HashMap<>();
    private final Map<UUID, Double> warmth = new HashMap<>();
    private final Map<UUID, Long> lastCold = new HashMap<>();
    /** Combat Achievement conditions: per player (healed a pyromancer, fixed a brazier) and for the whole fight. */
    private final Map<UUID, java.util.Set<String>> caPlayerFlags = new HashMap<>();
    private final java.util.Set<String> caRunFlags = new java.util.HashSet<>();
    private final List<Object[]> snowfalls = new ArrayList<>();
    /** A pending attack on a brazier: {brazier index, kind (0 shatter, 1 snuff out, 2 pyromancer), due tick}. */
    private final List<int[]> brazierAttacks = new ArrayList<>();
    private int energy;
    private int maxEnergy;
    private static boolean hooked;

    public WintertodtSession(int id, int slot, ServerLevel world, String variant) {
        super(Minigame.WINTERTODT, id, slot, world, variant);
    }

    /** Eating (food that heals 4+) and drinking rejuvenation potions restore a player's warmth in the prison. */
    private static void hook() {
        if (hooked) {
            return;
        }
        hooked = true;
        com.runecraft.content.GameEvents.onActivity((p, key) -> {
            if (!key.startsWith("eat:") || !(MinigameManager.session(p) instanceof WintertodtSession s)) {
                return;
            }
            String id = key.substring(4);
            if (id.equals("rejuvenation_potion")) {
                s.warm(p, Wintertodt.POTION_WARMTH);
            } else {
                com.runecraft.osrs.ItemDef def = com.runecraft.osrs.ItemCatalog.get(id);
                if (def != null && def.heal() >= Wintertodt.FOOD_MIN_HEAL) {
                    s.warm(p, Wintertodt.FOOD_WARMTH);
                }
            }
        });
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
                set(x, 0, z, Blocks.BLUE_ICE.defaultBlockState());
            }
        }
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayer player, int index) {
        return rel(index % 5 - 2, 1, 19);
    }

    @Override
    protected void start() {
        hook();
        maxEnergy = Wintertodt.energy(playerCount());
        energy = maxEnergy;
        Mob todt = spawn("wintertodt", rel(0, 1, 0));
        if (todt != null) {
            todt.setNoAi(true);
        }
        for (ServerPlayer p : online()) {
            warmth.put(p.getUUID(), (double) Wintertodt.WARMTH_MAX);
            if (warmItems(p) < 4) {
                Msg.game(p, "You feel the cold wind strike you as you enter; perhaps you should find some warmer clothes.");
            }
        }
        broadcastRed("The Wintertodt awakens! Light the braziers so the pyromancers can subdue it.");
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

    private void addPoints(ServerPlayer p, int amount) {
        points.merge(p.getUUID(), amount, Integer::sum);
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

    /** A random short delay before a relit brazier's (or healed pyromancer's) channelling resumes. */
    private int resumeDelay() {
        return Wintertodt.RESUME_MIN_TICKS + random.nextInt(Wintertodt.RESUME_MAX_TICKS - Wintertodt.RESUME_MIN_TICKS + 1);
    }

    @Override
    protected boolean onBlock(ServerPlayer player, String blockId, BlockPos pos) {
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
                // OSRS (9 October 2024): picking bruma herbs no longer gives Farming experience.
                if (Inv.hasRoomFor(player, "bruma_herb")) {
                    Inv.give(player, "bruma_herb", 1);
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

    private static boolean hasAnyAxe(ServerPlayer p) {
        for (String tier : new String[]{"bronze", "iron", "steel", "black", "mithril", "adamant", "rune", "dragon"}) {
            if (Inv.has(p, tier + "_axe", 1)) {
                return true;
            }
        }
        return false;
    }

    private void crate(ServerPlayer player) {
        int herbs = Inv.count(player, "bruma_herb");
        if (herbs > 0) {
            Inv.remove(player, "bruma_herb", herbs);
            Inv.give(player, "rejuvenation_potion", herbs);
            xp(player, Skill.HERBLORE, Wintertodt.potionXp(level(player, Skill.HERBLORE)) * herbs);
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

    private void brazier(ServerPlayer player, int i) {
        // A hurt pyromancer comes first: her 'Help' option heals her fully with a dose of rejuvenation potion.
        if (pyromancerHp[i] < Wintertodt.PYROMANCER_HP && Inv.has(player, "rejuvenation_potion", 1)) {
            Inv.remove(player, "rejuvenation_potion", 1);
            if (pyromancerHp[i] <= 0) {
                caPlayerFlags.computeIfAbsent(player.getUUID(), u -> new java.util.HashSet<>())
                        .add(com.runecraft.osrs.content.systems.CombatAchievements.FLAG_HEALED_PYROMANCER);
                drainIn[i] = resumeDelay();
            }
            pyromancerHp[i] = Wintertodt.PYROMANCER_HP;
            addPoints(player, Wintertodt.POINTS_HEAL_PYROMANCER);
            Msg.game(player, "You help the pyromancer back to full health.");
            return;
        }
        switch (braziers[i]) {
            case BROKEN -> {
                if (!Inv.has(player, "hammer", 1)) {
                    Msg.game(player, "You need a hammer to fix the brazier.");
                    return;
                }
                braziers[i] = State.UNLIT;
                caPlayerFlags.computeIfAbsent(player.getUUID(), u -> new java.util.HashSet<>()).add(com.runecraft.osrs.content.systems.CombatAchievements.FLAG_FIXED_BRAZIER);
                addPoints(player, Wintertodt.POINTS_FIX);
                xp(player, Skill.CONSTRUCTION, Wintertodt.fixXp(level(player, Skill.CONSTRUCTION)));
                Msg.game(player, "You fix the brazier.");
            }
            case UNLIT -> {
                if (pyromancerHp[i] <= 0) {
                    Msg.game(player, "The pyromancer is down! Help her with a rejuvenation potion before relighting.");
                    return;
                }
                if (!Inv.has(player, "tinderbox", 1) && !"bruma_torch".equals(Inv.idOf(player.getMainHandItem()))) {
                    Msg.game(player, "You need a tinderbox to light the brazier.");
                    return;
                }
                braziers[i] = State.LIT;
                drainIn[i] = resumeDelay();
                addPoints(player, Wintertodt.POINTS_LIGHT);
                xp(player, Skill.FIREMAKING, Wintertodt.lightXp(level(player, Skill.FIREMAKING)));
                world.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1f, 1f);
                Msg.game(player, "You light the brazier.");
            }
            case LIT -> {
                Msg.action(player, "You start feeding the brazier.");
                ActionManager.start(player, (p, t) -> {
                    if (braziers[i] != State.LIT || isEnded()) {
                        return false;
                    }
                    // Feeding gives points and experience; it doesn't make the fight go faster (wiki).
                    if (Inv.remove(p, "bruma_kindling", 1) > 0) {
                        addPoints(p, Wintertodt.POINTS_KINDLING);
                        xp(p, Skill.FIREMAKING, Wintertodt.burnKindlingXp(level(p, Skill.FIREMAKING)));
                        return true;
                    }
                    if (Inv.remove(p, "bruma_root", 1) > 0) {
                        addPoints(p, Wintertodt.POINTS_ROOT);
                        xp(p, Skill.FIREMAKING, Wintertodt.burnRootXp(level(p, Skill.FIREMAKING)));
                        return true;
                    }
                    Msg.game(p, "You have run out of bruma roots.");
                    return false;
                });
            }
        }
    }

    /** Item ids the player wears: the armour and hand slots, the extra slots (cape, neck, gloves, ring) and quest gear. */
    private static List<String> worn(ServerPlayer p) {
        List<String> ids = new ArrayList<>();
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
                EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
            ItemStack st = p.getItemBySlot(slot);
            if (!st.isEmpty()) {
                ids.add(Inv.idOf(st));
            }
        }
        for (ItemStack st : com.runecraft.content.systems.SysState.of(p).worn.values()) {
            if (st != null && !st.isEmpty()) {
                ids.add(Inv.idOf(st));
            }
        }
        ids.addAll(com.runecraft.combat.HitCalcBridge.extraWornIds.apply(p));
        return ids;
    }

    /** Warm items worn (OSRS Wiki "Warmth"), up to four. */
    private static int warmItems(ServerPlayer p) {
        return Wintertodt.warmItems(worn(p));
    }

    private double warmthOf(ServerPlayer p) {
        return warmth.getOrDefault(p.getUUID(), (double) Wintertodt.WARMTH_MAX);
    }

    /** Restores warmth (food, potions). */
    private void warm(ServerPlayer p, int percent) {
        warmth.put(p.getUUID(), Math.min(Wintertodt.WARMTH_MAX, warmthOf(p) + percent));
        Msg.action(p, "You feel warmer. Warmth: " + (int) Math.floor(warmthOf(p)) + "%");
    }

    /** The cold takes warmth; at 0% the player dies (OSRS: instant death, an unsafe one). */
    private void chill(ServerPlayer p, int percent) {
        if (percent <= 0) {
            return;
        }
        double left = warmthOf(p) - percent;
        warmth.put(p.getUUID(), Math.max(0, left));
        com.runecraft.network.ServerNetworking.hitsplat(p, percent, com.runecraft.network.Net.SPLAT_DAMAGE);
        if (left <= 0) {
            Msg.red(p, "The cold of the Wintertodt overwhelms you.");
            warmth.put(p.getUUID(), (double) Wintertodt.WARMTH_MAX);
            CombatHandler.applyDamage(null, p, (int) Math.ceil(p.getHealth()) + 1);
        }
    }

    /** Whether the player stands in the safe area by the doors, out of the Wintertodt's reach. */
    private boolean safe(ServerPlayer p) {
        return p.getZ() - origin.getZ() >= SAFE_FROM_Z;
    }

    @Override
    protected void gameTick() {
        if (energy <= 0) {
            broadcastRed("The Wintertodt is subdued!");
            end(true);
            return;
        }
        // The pyromancers at the lit braziers drain the Wintertodt; with every brazier out it regains energy.
        for (int i = 0; i < 4; i++) {
            BlockPos b = rel(BRAZIERS[i][0], 2, BRAZIERS[i][1]);
            if (braziers[i] == State.LIT) {
                world.sendParticles(ParticleTypes.FLAME, b.getX() + 0.5, b.getY(), b.getZ() + 0.5, 4, 0.2, 0.3, 0.2, 0.02);
                if (pyromancerHp[i] > 0 && --drainIn[i] <= 0) {
                    drainIn[i] = Wintertodt.PYROMANCER_DRAIN_TICKS;
                    energy = Math.max(0, energy - 1);
                    world.sendParticles(ParticleTypes.HAPPY_VILLAGER, b.getX() + 0.5, b.getY() + 1, b.getZ() + 0.5, 6, 0.3, 0.6, 0.3, 0.05);
                }
            } else if (braziers[i] == State.BROKEN) {
                world.sendParticles(ParticleTypes.SMOKE, b.getX() + 0.5, b.getY(), b.getZ() + 0.5, 2, 0.2, 0.2, 0.2, 0.01);
            }
        }
        if (lit() == 0) {
            if (--regainIn <= 0) {
                regainIn = Wintertodt.REGAIN_TICKS;
                energy = Math.min(maxEnergy, energy + 1);
            }
        } else {
            regainIn = 1 + random.nextInt(Wintertodt.REGAIN_TICKS); // a random delay before it starts regaining
        }
        List<ServerPlayer> online = online();
        // Warmth regenerates: 8% a minute plus 1% per warm item.
        for (ServerPlayer p : online) {
            warmth.put(p.getUUID(), Math.min(Wintertodt.WARMTH_MAX, warmthOf(p) + Wintertodt.warmthRegenPerTick(warmItems(p))));
        }
        // The standard cold, more often while the Wintertodt still has a lot of energy.
        for (ServerPlayer p : online) {
            long last = lastCold.getOrDefault(p.getUUID(), -1000L);
            if (safe(p) || ticks - last < Wintertodt.STANDARD_COOLDOWN_TICKS) {
                continue;
            }
            lastCold.put(p.getUUID(), ticks);
            if (!Wintertodt.attacksPlayers(random.nextInt(maxEnergy + 1), energy)) {
                continue;
            }
            chill(p, Wintertodt.standardDamage(level(p, Skill.FIREMAKING), warmItems(p), lit()));
            if (random.nextInt(3) == 0) {
                ActionManager.cancel(p); // approx: it interrupts fletching and feeding, not chopping
                Msg.action(p, "The cold of the Wintertodt seeps into your bones.");
            }
        }
        // Snowfall: a telegraphed 3x3 area attack centred on a player.
        if (ticks % SNOWFALL_TICKS == 0 && Wintertodt.attacksPlayers(random.nextInt(maxEnergy + 1), energy)) {
            List<ServerPlayer> targets = online.stream().filter(p -> !safe(p)).toList();
            if (!targets.isEmpty()) {
                ServerPlayer target = targets.get(random.nextInt(targets.size()));
                snowfalls.add(new Object[]{target.position(), ticks + WARNING_TICKS});
            }
        }
        snowfalls.removeIf(s -> {
            Vec3 at = (Vec3) s[0];
            world.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y + 3, at.z, 20, 1.2, 0.5, 1.2, 0.01);
            if (ticks < (Long) s[1]) {
                return false;
            }
            for (ServerPlayer p : online()) {
                if (Math.abs(p.getX() - at.x) <= 3 && Math.abs(p.getZ() - at.z) <= 3 && !safe(p)) {
                    chill(p, Wintertodt.areaDamage(level(p, Skill.FIREMAKING), warmItems(p)));
                }
            }
            return true;
        });
        // Attacks on the braziers and pyromancers, more often as its energy runs low.
        if (ticks % BRAZIER_ATTACK_TICKS == 0 && Wintertodt.attacksBraziers(random.nextInt(maxEnergy + 1), energy)) {
            List<Integer> litOnes = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                if (braziers[i] == State.LIT && pyromancerHp[i] > 0) {
                    litOnes.add(i);
                }
            }
            if (!litOnes.isEmpty()) {
                int i = litOnes.get(random.nextInt(litOnes.size()));
                brazierAttacks.add(new int[]{i, random.nextInt(3), (int) ticks + WARNING_TICKS}); // approx: each kind a third
            }
        }
        brazierAttacks.removeIf(a -> {
            int i = a[0];
            BlockPos b = rel(BRAZIERS[i][0], 3, BRAZIERS[i][1]);
            world.sendParticles(ParticleTypes.SNOWFLAKE, b.getX() + 0.5, b.getY() + 1, b.getZ() + 0.5, a[1] == 0 ? 20 : 6, 0.6, 0.4, 0.6, 0.01);
            if (ticks < a[2]) {
                return false;
            }
            switch (a[1]) {
                case 0 -> {
                    if (braziers[i] != State.BROKEN) {
                        braziers[i] = State.BROKEN;
                        broadcastRed("A brazier shatters!");
                        if (java.util.Arrays.stream(braziers).allMatch(s -> s == State.BROKEN)) {
                            caRunFlags.add(com.runecraft.osrs.content.systems.CombatAchievements.FLAG_ALL_BRAZIERS_BROKEN);
                        }
                        for (ServerPlayer p : online()) {
                            if (p.blockPosition().distSqr(rel(BRAZIERS[i][0], 1, BRAZIERS[i][1])) <= 9) {
                                chill(p, Wintertodt.brazierDamage(level(p, Skill.FIREMAKING), warmItems(p)));
                            }
                        }
                    }
                }
                case 1 -> {
                    if (braziers[i] == State.LIT) {
                        braziers[i] = State.UNLIT;
                        broadcast("The cold of the Wintertodt extinguishes a brazier.");
                    }
                }
                default -> {
                    pyromancerHp[i] = Math.max(0, pyromancerHp[i] - Wintertodt.PYROMANCER_HIT);
                    if (pyromancerHp[i] <= 0) {
                        braziers[i] = braziers[i] == State.LIT ? State.UNLIT : braziers[i];
                        caRunFlags.add(com.runecraft.osrs.content.systems.CombatAchievements.FLAG_PYROMANCER_FELL);
                        broadcastRed("A pyromancer has fallen!");
                    } else {
                        broadcast("The cold of the Wintertodt hurts a pyromancer.");
                    }
                }
            }
            return true;
        });
    }

    @Override
    public java.util.Set<String> caFlags(ServerPlayer player) {
        java.util.Set<String> flags = new java.util.HashSet<>(caRunFlags);
        flags.addAll(caPlayerFlags.getOrDefault(player.getUUID(), java.util.Set.of()));
        if (warmItems(player) >= 4) {
            flags.add(com.runecraft.osrs.content.systems.CombatAchievements.FLAG_WARM_GEAR);
        }
        if (points.getOrDefault(player.getUUID(), 0) >= 3000) {
            flags.add(com.runecraft.osrs.content.systems.CombatAchievements.FLAG_POINTS_3000);
        }
        return flags;
    }

    @Override
    protected void onEnd(boolean success) {
        if (!success) {
            return;
        }
        for (ServerPlayer p : online()) {
            int pts = points.getOrDefault(p.getUUID(), 0);
            int rolls = Wintertodt.rolls(pts, random.nextDouble());
            if (rolls > 0) {
                xp(p, Skill.FIREMAKING, Wintertodt.completionXp(level(p, Skill.FIREMAKING)));
                Inv.give(p, MinigameItemUse.container("supply_crate", rolls));
                int kc = MinigameData.of(p).incrementStat(p.getUUID(), "kc:" + game.id());
                Msg.game(p, "You have helped to subdue the Wintertodt! You earned " + pts + " points and a supply crate (" + rolls + " rolls). "
                        + "Your subdued Wintertodt count is: " + kc + ".");
            } else {
                Msg.game(p, "You did not earn enough points (" + pts + "/500) to be worthy of a gift from the citizens of Kourend this time.");
            }
        }
    }

    @Override
    protected List<String> hud(ServerPlayer player) {
        return List.of("Wintertodt", "Wintertodt's Energy: " + (maxEnergy == 0 ? 100 : energy * 100 / maxEnergy) + "%",
                "Warmth: " + (int) Math.floor(warmthOf(player)) + "%", "Points: " + points.getOrDefault(player.getUUID(), 0),
                "Braziers lit: " + lit() + " / 4", "Warm clothing: " + warmItems(player) + " / 4");
    }

    // ------------------------------------------------------------------ self-test

    /** Self-test view: energy, braziers lit, the player's warmth and points. */
    public record View(int energy, int lit, double warmth, int points) {
    }

    public View selfTestView(ServerPlayer p) {
        return new View(energy, lit(), warmthOf(p), points.getOrDefault(p.getUUID(), 0));
    }

    /** Self-test: lights every brazier as a player with a tinderbox would. */
    public void selfTestLightAll(ServerPlayer p) {
        Inv.give(p, "tinderbox", 1);
        for (int i = 0; i < 4; i++) {
            if (braziers[i] == State.UNLIT) {
                brazier(p, i);
            }
        }
    }

    /** Self-test: one standard cold hit on the player (wherever they stand). */
    public void selfTestChill(ServerPlayer p) {
        chill(p, Wintertodt.standardDamage(level(p, Skill.FIREMAKING), warmItems(p), lit()));
    }

    /** Self-test: the player eats as a player would ("eat:" activity). */
    public void selfTestWarm(ServerPlayer p, String food) {
        com.runecraft.content.GameEvents.fireActivity(p, "eat:" + food);
    }
}
