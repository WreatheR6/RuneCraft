package com.runecraft.content.minigames.games;

import com.runecraft.combat.CombatHandler;
import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.content.minigames.RewardShopService;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.CombatStyle;
import com.runecraft.osrs.Spell;
import com.runecraft.osrs.content.minigames.BaRules;
import com.runecraft.osrs.content.minigames.BaRules.Call;
import com.runecraft.osrs.content.minigames.BaWaves;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameCurrencies;
import com.runecraft.osrs.content.minigames.MinigameRules.BarbarianAssault;
import com.runecraft.osrs.content.minigames.RewardShop;
import com.runecraft.osrs.content.minigames.RewardShops;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * Self-test driver for Barbarian Assault ({@code com.runecraft.selftest}, "minigame:barbarian_assault_run" in real-player mode):
 * a real player enters as an attacker and, alone, plays wave 1 through the real session (every role's job falls to them):
 * the wave's Penance by the wiki table, the attacker's style call (a wrong style deals nothing; the right melee stance, arrow and
 * spell element add the level bonus), the reserve Penance, eggs of the called colour into the collection bag, loading and firing
 * the egg launcher, bait luring a runner onto the trap (and the trap breaking and being repaired), a runner escaping, poisoned
 * food on a Penance healer (the wrong food a penalty), the healing vial and the spring; the wave's honour points by the wiki
 * formula; then wave 10: the Penance first, the queen out of the trapdoor, weapons doing nothing, a yellow egg picked up, the
 * omega egg made at the poison crater, the mushroom and the lava crater, eight omega eggs from the launcher, the queen's 80 / 5
 * honour points and queen kill; and last a role level and a fighter torso bought with honour points. Every broken step throws an
 * {@link AssertionError}.
 */
public final class BarbarianAssaultSelfTest {
    private final ServerPlayer player;
    private final UUID id;
    private final MinigameData data;

    private BarbarianAssaultSelfTest(ServerPlayer player) {
        this.player = player;
        this.id = player.getUUID();
        this.data = MinigameData.of(player);
    }

    public static String run(ServerPlayer player) {
        return new BarbarianAssaultSelfTest(player).walk();
    }

    private static void expect(boolean ok, String what) {
        if (!ok) {
            throw new AssertionError(what);
        }
    }

    private void heal() {
        if (player.getHealth() < player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private void tick(MinigameSession s, int n) {
        for (int i = 0; i < n && !s.isEnded(); i++) {
            MinigameManager.selfTestTick(s);
            heal();
        }
    }

    private int honour(int role) {
        return data.points(id, "ba_" + BarbarianAssault.ROLES[role]);
    }

    private void setHonour(int role, int amount) {
        data.addPoints(id, "ba_" + BarbarianAssault.ROLES[role], amount - honour(role));
    }

    private BarbarianAssaultSession start(String entrance) {
        MinigameSession s = MinigameManager.selfTestStart(Minigame.BARBARIAN_ASSAULT, entrance, player);
        if (!(s instanceof BarbarianAssaultSession ba)) {
            s.end(false);
            throw new AssertionError("Barbarian Assault started a " + s.getClass().getSimpleName());
        }
        return ba;
    }

    private void tp(BlockPos at) {
        player.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
    }

    private void tp(double x, double y, double z) {
        player.teleportTo(x, y, z);
    }

    private static void end(MinigameSession s) {
        if (!s.isEnded()) {
            s.end(false);
        }
    }

    // ------------------------------------------------------------------ the walk

    private String walk() {
        for (String role : BarbarianAssault.ROLES) {
            data.setStat(id, "ba_level_" + role, 1);
        }
        data.setStat(id, BarbarianAssaultSession.WAVE_STAT, 0);
        player.getInventory().clearContent();
        StringBuilder out = new StringBuilder();
        BarbarianAssaultSession s = start("barbarian_assault_attacker_entrance");
        try {
            out.append(waveOne(s));
        } finally {
            end(s);
        }
        expect(MinigameManager.session(player) == null, "the player is still bound to the session after it ended");
        for (String item : BarbarianAssaultSession.KEPT_ITEMS) {
            expect(!Inv.has(player, item, 1), "leaving kept the arena's " + item);
        }
        data.setStat(id, BarbarianAssaultSession.WAVE_STAT, BaWaves.WAVES);
        BarbarianAssaultSession q = start("barbarian_assault_attacker_entrance");
        try {
            out.append("; ").append(queenWave(q));
        } finally {
            end(q);
        }
        out.append("; ").append(rewards());
        return out.toString();
    }

    private String waveOne(BarbarianAssaultSession s) {
        expect(s.role(player) == BaRules.ATTACKER, "the attacker entrance gave role " + s.role(player));
        expect(s.wave() == 1, "a new round starts on wave " + s.wave());
        expect(Inv.has(player, "attacker_horn", 1) && Inv.has(player, "attacker_icon", 1), "no attacker horn or icon");
        expect(Inv.has(player, "collection_bag", 1), "alone, the attacker also collects: no collection bag");
        expect(!s.awardsXp(), "Barbarian Assault gives combat experience");
        expect(com.runecraft.combat.PrayerService.minigameRule.apply(player, com.runecraft.osrs.Prayer.PROTECT_FROM_MELEE) != null,
                "prayers work in Barbarian Assault");
        tick(s, 12);
        expect(s.phase() == BarbarianAssaultSession.Phase.RUNNING, "wave 1 did not start (phase " + s.phase() + ")");
        // Alone, every call comes by itself.
        expect(s.told(player) == s.callValue(Call.STYLE), "the style call didn't come to a lone attacker");
        tick(s, 11);
        for (int t = 0; t < BaWaves.TYPES; t++) {
            int alive = s.penance(t).size();
            expect(alive == BaWaves.initial(1, t), "wave 1 has " + alive + " " + BarbarianAssault.PENANCE[t] + " out (wiki "
                    + BaWaves.initial(1, t) + ")");
            expect(s.reserve(t) == BaWaves.reserve(1, t), BarbarianAssault.PENANCE[t] + " reserve " + s.reserve(t));
        }
        LivingEntity fighter = s.penance(BaWaves.FIGHTER).get(0);
        expect((int) fighter.getMaxHealth() == BaWaves.stats(1, BaWaves.FIGHTER).hitpoints(), "wave 1 fighter has "
                + fighter.getMaxHealth() + " hitpoints");

        // The attacker's style: controlled / bullet / wind.
        s.setCall(Call.STYLE, 0);
        PlayerData pd = Players.data(player);
        CombatStyle styleBefore = pd.style();
        Spell autocastBefore = pd.autocast();
        try {
            pd.setStyle(CombatStyle.ACCURATE);
            int wrong = CombatHandler.hookAfterRoll(player, fighter, AttackType.CRUSH, 5);
            expect(wrong == 0, "a wrong-style hit dealt " + wrong);
            expect(s.waveStats().wrongAttacks() == 1, "the wrong hit wasn't counted");
            pd.setStyle(CombatStyle.CONTROLLED);
            int right = CombatHandler.hookAfterRoll(player, fighter, AttackType.CRUSH, 5);
            expect(right == 5 + BaRules.attackerBonus(1), "a controlled hit dealt " + right + " (5 + level 1 bonus)");
            int miss = CombatHandler.hookAfterRoll(player, fighter, AttackType.CRUSH, 0);
            expect(miss == BaRules.attackerBonus(1), "a missed controlled hit dealt " + miss + " (the bonus lands even on a miss)");
            BarbarianAssaultSession.LAST_ARROW.put(id, "bullet_arrow");
            expect(CombatHandler.hookAfterRoll(player, fighter, AttackType.RANGED, 3) == 4, "a bullet arrow on the bullet call");
            BarbarianAssaultSession.LAST_ARROW.put(id, "rune_arrow");
            expect(CombatHandler.hookAfterRoll(player, fighter, AttackType.RANGED, 3) == 0, "a rune arrow hurt a Penance fighter");
            expect(s.waveStats().wrongAttacks() == 1, "other arrows counted as wrong hits");
            pd.setAutocast(Spell.WIND_STRIKE);
            expect(CombatHandler.hookAfterRoll(player, fighter, AttackType.MAGIC, 2) == 3, "Wind Strike on the wind call");
            pd.setAutocast(Spell.FIRE_STRIKE);
            expect(CombatHandler.hookAfterRoll(player, fighter, AttackType.MAGIC, 2) == 0, "Fire Strike on the wind call");
        } finally {
            pd.setStyle(styleBefore);
            pd.setAutocast(autocastBefore);
            BarbarianAssaultSession.LAST_ARROW.remove(id);
        }
        // Weapons can't touch runners or Penance healers.
        LivingEntity runner = s.penance(BaWaves.RUNNER).get(0);
        expect(CombatHandler.applyDamage(player, runner, 10) == 0, "a weapon hurt a Penance runner");
        expect(CombatHandler.applyDamage(player, s.penance(BaWaves.HEALER).get(0), 10) == 0, "a weapon hurt a Penance healer");

        // Fighters and rangers killed release the reserve and drop eggs.
        s.setCall(Call.EGG, 0); // red
        BlockPos dropped = fighter.blockPosition();
        expect(CombatHandler.applyDamage(player, fighter, 100) > 0 && !fighter.isAlive(), "the attacker couldn't kill a fighter");
        expect(s.reserve(BaWaves.FIGHTER) == BaWaves.reserve(1, BaWaves.FIGHTER) - 1, "a fighter's death didn't release the reserve");
        List<ItemEntity> eggs = player.serverLevel().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(dropped).inflate(3),
                e -> e.isAlive() && e.getTags().contains(BarbarianAssaultSession.EGG_TAG));
        expect(eggs.size() == 3 * BaRules.EGGS_PER_COLOUR, "a dead fighter dropped " + eggs.size() + " eggs (two of each colour)");
        List<ItemEntity> reds = eggs.stream().filter(e -> Inv.idOf(e.getItem()).equals("red_egg")).toList();
        List<ItemEntity> blues = eggs.stream().filter(e -> Inv.idOf(e.getItem()).equals("blue_egg")).toList();
        tp(blues.get(0).getX(), blues.get(0).getY(), blues.get(0).getZ());
        tick(s, 1);
        expect(blues.stream().allMatch(ItemEntity::isAlive), "a blue egg was picked up on the red call");
        for (ItemEntity red : reds) {
            tp(red.getX(), red.getY(), red.getZ());
            tick(s, 1);
        }
        int[] bag = s.bag(player);
        expect(bag[0] == 2, "the collection bag holds " + bag[0] + " red eggs after walking over two");
        expect(s.waveStats().eggsCollected() == 2, "eggs collected: " + s.waveStats().eggsCollected());

        // Load the egg launcher and fire red eggs at the rangers (3 damage each).
        BlockPos launcher = s.at(BarbarianAssaultSession.LAUNCHERS[0]);
        tp(launcher.south());
        s.onBlock(player, "ba_egg_launcher", launcher);
        expect(s.hopper(0) == 2 && s.bag(player)[0] == 0, "loading left the hopper with " + s.hopper(0) + " red eggs");
        float rangerHp = 0;
        for (LivingEntity r : s.penance(BaWaves.RANGER)) {
            rangerHp += r.getHealth();
        }
        s.fire(player, "red", "ranger");
        s.fire(player, "red", "ranger");
        expect(s.hopper(0) == 0, "two red eggs fired left " + s.hopper(0));
        float rangerHpAfter = 0;
        for (LivingEntity r : s.penance(BaWaves.RANGER)) {
            rangerHpAfter += r.getHealth();
        }
        expect(rangerHp - rangerHpAfter >= 2 * BaRules.RED_DAMAGE, "two red eggs took " + (rangerHp - rangerHpAfter) + " off the rangers");

        // A runner reaching the lure cave escapes and comes back out of the north cave.
        List<LivingEntity> runners = s.penance(BaWaves.RUNNER);
        expect(!runners.isEmpty(), "no runner to escape");
        int pendingBefore = s.pending(BaWaves.RUNNER);
        int outBefore = runners.size();
        BlockPos cave = s.at(BarbarianAssaultSession.LURE_CAVE);
        runners.get(0).teleportTo(cave.getX() + 0.5, cave.getY(), cave.getZ() - 1.5);
        tick(s, 1);
        expect(s.waveStats().runnersEscaped() == 1, "a runner at the lure cave didn't escape");
        expect(s.pending(BaWaves.RUNNER) + s.penance(BaWaves.RUNNER).size() == pendingBefore + outBefore, "the escaped runner doesn't come back");
        // The defender's bait lures runners onto the east trap: two runners break it, a hammer and logs repair it.
        int traps = trapRunners(s);

        // The healer's poisoned food: the wrong one is a penalty, the called one poisons a Penance healer to death.
        String healerNote = poisonHealer(s);

        // The healing vial and the spring.
        BlockPos spring = s.at(BarbarianAssaultSession.SPRING);
        tp(spring.north());
        expect(Inv.has(player, BaRules.HEALING_VIAL, 1), "the healer item machine gave no healing vial");
        s.onBlock(player, "ba_healer_spring", spring);
        expect(Inv.has(player, BaRules.HEALING_VIAL + "_4", 1), "the spring didn't fill the healing vial");
        expect(s.useVial(player, player, BaRules.HEALING_VIAL + "_4") && Inv.has(player, BaRules.HEALING_VIAL + "_3", 1),
                "a dose of the healing vial wasn't used");
        // The converter needs collector level 2.
        s.onBlock(player, "ba_collector_converter", s.at(new int[]{-6, 20}));

        // Finish the wave: kill the rest through the session's own damage as reserves come out.
        int before = honour(BaRules.ATTACKER);
        for (int round = 0; round < 40 && s.wave() == 1; round++) {
            for (int t = 0; t < BaWaves.TYPES; t++) {
                for (LivingEntity e : s.penance(t)) {
                    s.selfTestKill(e);
                }
            }
            tick(s, 1);
            if (s.wave() == 1) {
                tick(s, BaWaves.SPAWN_INTERVAL_TICKS - 1);
            }
        }
        expect(s.wave() == 2 && s.phase() == BarbarianAssaultSession.Phase.WAITING, "wave 1 never ended (wave " + s.wave() + ", "
                + s.phase() + ")");
        BaRules.WaveStats st = s.waveStats();
        expect(st.fightersKilled() == BaWaves.total(1, BaWaves.FIGHTER) && st.rangersKilled() == BaWaves.total(1, BaWaves.RANGER)
                && st.healersKilled() == BaWaves.total(1, BaWaves.HEALER), "wave 1 kills " + st);
        int want = BaRules.wavePoints(BaRules.ATTACKER, 1, st, BarbarianAssaultSession.kandarinHard(player));
        int got = honour(BaRules.ATTACKER) - before;
        expect(got == want && got > 0, "wave 1 paid " + got + " attacker honour (formula: " + want + " for " + st + ")");
        expect(BarbarianAssaultSession.waveReached(player) == 2, "the round's wave is " + BarbarianAssaultSession.waveReached(player));
        for (String item : List.of("bullet_arrow", "red_egg", "tofu", "poisoned_tofu", BaRules.HEALING_VIAL + "_3")) {
            expect(!Inv.has(player, item, 1), "the wave's " + item + " wasn't taken back");
        }
        expect(Inv.has(player, "attacker_horn", 1), "the horn was taken between waves");
        return "wave 1 as a lone attacker: " + got + " honour (" + st.eggsCollected() + " eggs, " + st.runnersEscaped() + " escaped, "
                + st.wrongAttacks() + " wrong hit, " + st.wrongPoison() + " wrong food), " + traps + " trapped, " + healerNote;
    }

    /** Lures runners onto the east trap with good bait until it breaks, then repairs it. Returns how many it killed. */
    private int trapRunners(BarbarianAssaultSession s) {
        BlockPos machine = s.at(new int[]{6, 20});
        tp(machine.north());
        s.onBlock(player, "ba_defender_machine", machine);
        expect(Inv.count(player, "tofu") > 0 && Inv.count(player, "worms") > 0, "the defender item machine gave no bait");
        s.setCall(Call.BAIT, 0); // tofu
        BlockPos trap = s.at(BarbarianAssaultSession.TRAPS[0]);
        int killed = 0;
        for (int n = 0; n < BaRules.TRAP_CHARGES; n++) {
            List<LivingEntity> runners = s.penance(BaWaves.RUNNER);
            for (int wait = 0; runners.isEmpty() && wait < 4; wait++) {
                tick(s, BaWaves.SPAWN_INTERVAL_TICKS);
                runners = s.penance(BaWaves.RUNNER);
            }
            expect(!runners.isEmpty(), "no runner to lure onto the trap");
            LivingEntity runner = runners.get(0);
            int before = s.waveStats().runnersKilled();
            ItemEntity bait = new ItemEntity(player.serverLevel(), trap.getX() + 0.5, trap.getY(), trap.getZ() - 0.5, Inv.stack("tofu", 1));
            bait.setPos(trap.getX() + 0.5, trap.getY(), trap.getZ() - 0.5);
            bait.setThrower(player);
            player.serverLevel().addFreshEntity(bait);
            Inv.remove(player, "tofu", 1);
            tick(s, 1);
            expect(bait.getTags().contains(BarbarianAssaultSession.GOOD_BAIT), "tofu dropped on the tofu call isn't good bait");
            for (int t = 0; t < 15 && s.waveStats().runnersKilled() == before; t++) {
                if (runner.isAlive() && bait.isAlive()) {
                    runner.teleportTo(bait.getX(), bait.getY(), bait.getZ());
                }
                tick(s, 1);
            }
            expect(s.waveStats().runnersKilled() == before + 1 && !bait.isAlive(), "a runner eating good bait at the trap didn't die");
            killed++;
        }
        expect(s.trapCharges(0) == 0, "two runners left the trap with " + s.trapCharges(0) + " charges");
        BlockPos logs = s.at(BarbarianAssaultSession.LOG_PILE);
        s.onBlock(player, "ba_hammer", s.at(BarbarianAssaultSession.HAMMER));
        s.onBlock(player, "ba_log_pile", logs);
        expect(Inv.has(player, "hammer", 1) && Inv.has(player, "logs", 1), "no hammer or logs from the arena");
        s.onBlock(player, "ba_runner_trap", trap);
        expect(s.trapCharges(0) == BaRules.TRAP_CHARGES, "the repaired trap has " + s.trapCharges(0) + " charges");
        return killed;
    }

    private String poisonHealer(BarbarianAssaultSession s) {
        BlockPos machine = s.at(new int[]{9, 20});
        tp(machine.north());
        s.onBlock(player, "ba_healer_machine", machine);
        expect(Inv.count(player, "poisoned_tofu") > 0 && Inv.count(player, "poisoned_meat") > 0, "the healer item machine gave no food");
        s.setCall(Call.POISON, 0); // tofu
        List<LivingEntity> healers = s.penance(BaWaves.HEALER);
        for (int wait = 0; healers.isEmpty() && wait < 4; wait++) {
            tick(s, BaWaves.SPAWN_INTERVAL_TICKS);
            healers = s.penance(BaWaves.HEALER);
        }
        expect(!healers.isEmpty(), "no Penance healer to poison");
        LivingEntity healer = healers.get(0);
        expect(s.useFood(player, healer, "poisoned_meat") && s.waveStats().wrongPoison() == 1, "the wrong food wasn't a penalty");
        expect(healer.getHealth() == healer.getMaxHealth(), "the wrong food hurt the healer");
        int before = s.waveStats().healersKilled();
        expect(s.useFood(player, healer, "poisoned_tofu"), "the called food wasn't used");
        expect(healer.getMaxHealth() - healer.getHealth() >= BaRules.FOOD_DAMAGE || !healer.isAlive(), "the called food didn't hit for 4");
        int ticks = 0;
        for (; ticks < 40 && healer.isAlive(); ticks++) {
            tick(s, 1);
        }
        expect(!healer.isAlive() && s.waveStats().healersKilled() == before + 1, "one poisoned tofu didn't kill a 27-hitpoint healer");
        return "a healer poisoned to death in " + ticks + " ticks";
    }

    // ------------------------------------------------------------------ wave 10

    private String queenWave(BarbarianAssaultSession s) {
        expect(s.wave() == BaWaves.WAVES, "a round at wave 10 started on wave " + s.wave());
        tick(s, 12);
        expect(s.phase() == BarbarianAssaultSession.Phase.RUNNING, "wave 10 did not start");
        // The queen only comes out once every Penance is dead.
        for (int round = 0; round < 40 && !(s.allOut() && s.penanceAlive() == 0); round++) {
            for (int t = 0; t < BaWaves.TYPES; t++) {
                for (LivingEntity e : s.penance(t)) {
                    s.selfTestKill(e);
                }
            }
            tick(s, BaWaves.SPAWN_INTERVAL_TICKS);
        }
        expect(s.allOut() && s.penanceAlive() == 0, "wave 10's Penance never all died");
        expect(s.queen() == null, "the queen came out before the Penance were dead");
        for (int t = 0; t < 60 && s.queen() == null; t++) {
            tick(s, 1);
        }
        Mob queen = s.queen();
        expect(queen != null && queen.isAlive(), "the Penance Queen never came out");
        expect((int) queen.getMaxHealth() == BaWaves.QUEEN.hitpoints(), "the queen has " + queen.getMaxHealth() + " hitpoints");
        expect(CombatHandler.applyDamage(player, queen, 50) == 0 && queen.getHealth() == queen.getMaxHealth(), "a weapon hurt the queen");
        // Yellow eggs come out next to her; a collector picks one up.
        ItemEntity yellow = null;
        for (int t = 0; t < 15 && yellow == null; t++) {
            tick(s, 1);
            yellow = player.serverLevel().getEntitiesOfClass(ItemEntity.class, s.box(), e -> e.isAlive()
                    && Inv.idOf(e.getItem()).equals(BaRules.YELLOW_EGG)).stream().findFirst().orElse(null);
        }
        expect(yellow != null, "the queen dropped no yellow egg");
        tp(yellow.getX(), yellow.getY(), yellow.getZ());
        tick(s, 1);
        expect(Inv.has(player, BaRules.YELLOW_EGG, 1), "the yellow egg wasn't picked up");
        // Accelerated: seven more yellow eggs for the eight omega eggs.
        Inv.give(player, BaRules.YELLOW_EGG, 7);
        tp(s.at(BarbarianAssaultSession.POISON_CRATER).east());
        s.onBlock(player, BaRules.POISON_CRATER, s.at(BarbarianAssaultSession.POISON_CRATER));
        s.onBlock(player, BaRules.PETRIFIED_MUSHROOM, s.at(BarbarianAssaultSession.PETRIFIED_MUSHROOM));
        s.onBlock(player, BaRules.LAVA_CRATER, s.at(BarbarianAssaultSession.LAVA_CRATER));
        expect(Inv.count(player, BaRules.OMEGA_EGG) == 8, "the crater, mushroom and crater made " + Inv.count(player, BaRules.OMEGA_EGG)
                + " omega eggs out of 8 yellow eggs");
        BlockPos launcher = s.at(BarbarianAssaultSession.LAUNCHERS[0]);
        tp(launcher.south());
        s.onBlock(player, "ba_egg_launcher", launcher);
        expect(s.hopper(3) == 8, "the hopper holds " + s.hopper(3) + " omega eggs");
        int[] before = new int[4];
        for (int r = 0; r < 4; r++) {
            before[r] = honour(r);
        }
        int queenKills = data.points(id, MinigameCurrencies.key(MinigameCurrencies.BA_QUEEN));
        int kc = data.stat(id, "kc:" + Minigame.BARBARIAN_ASSAULT.id());
        for (int egg = 0; egg < 8 && !s.isEnded(); egg++) {
            heal();
            s.fire(player, "omega", "queen");
            if (egg < 7) {
                expect(queen.isAlive(), (egg + 1) + " omega eggs killed the queen (wiki: 8)");
            }
        }
        expect(!queen.isAlive() && s.isEnded(), "eight omega eggs didn't kill the queen");
        boolean diary = BarbarianAssaultSession.kandarinHard(player);
        for (int r = 0; r < 4; r++) {
            int gained = honour(r) - before[r];
            expect(gained == BaRules.wave10(BaRules.ATTACKER, r, diary), BarbarianAssault.ROLES[r] + " got " + gained + " honour for the queen");
        }
        expect(data.points(id, MinigameCurrencies.key(MinigameCurrencies.BA_QUEEN)) == queenKills + 1, "no queen kill");
        expect(data.stat(id, "kc:" + Minigame.BARBARIAN_ASSAULT.id()) == kc + 1, "the queen kill count didn't go up");
        expect(BarbarianAssaultSession.waveReached(player) == 0, "the round didn't reset after the queen");
        return "the queen fell to 8 omega eggs (80 / 5 honour, a queen kill)";
    }

    // ------------------------------------------------------------------ rewards

    private String rewards() {
        // Accelerated: top the attacker up to the level 2 price.
        setHonour(BaRules.ATTACKER, Math.max(honour(BaRules.ATTACKER), BarbarianAssault.levelCost(2)));
        int before = honour(BaRules.ATTACKER);
        expect(BarbarianAssaultSession.buyLevel(player, BaRules.ATTACKER), "couldn't buy attacker level 2");
        expect(BarbarianAssaultSession.roleLevel(player, BaRules.ATTACKER) == 2, "attacker level is "
                + BarbarianAssaultSession.roleLevel(player, BaRules.ATTACKER));
        expect(honour(BaRules.ATTACKER) == before - 200, "level 2 cost " + (before - honour(BaRules.ATTACKER)));
        // A fighter torso: 375 in every role and a queen kill (earned above).
        for (int r = 0; r < 4; r++) {
            setHonour(r, Math.max(honour(r), 375));
        }
        int[] had = new int[4];
        for (int r = 0; r < 4; r++) {
            had[r] = honour(r);
        }
        int queenKills = data.points(id, MinigameCurrencies.key(MinigameCurrencies.BA_QUEEN));
        expect(queenKills >= 1, "no queen kill to spend");
        RewardShop shop = RewardShops.get("barbarian_assault");
        int index = -1;
        for (int i = 0; i < shop.entries().size(); i++) {
            if (shop.entries().get(i).item().equals("fighter_torso")) {
                index = i;
            }
        }
        expect(index >= 0, "Commander Connad sells no fighter torso");
        int torsos = Inv.count(player, "fighter_torso");
        RewardShopService.buy(player, "barbarian_assault", index, 1);
        expect(Inv.count(player, "fighter_torso") == torsos + 1, "the fighter torso wasn't bought");
        for (int r = 0; r < 4; r++) {
            expect(honour(r) == had[r] - 375, BarbarianAssault.ROLES[r] + " paid " + (had[r] - honour(r)) + " for the torso");
        }
        expect(data.points(id, MinigameCurrencies.key(MinigameCurrencies.BA_QUEEN)) == queenKills - 1, "the torso didn't take a queen kill");
        return "attacker level 2 and a fighter torso bought";
    }
}
