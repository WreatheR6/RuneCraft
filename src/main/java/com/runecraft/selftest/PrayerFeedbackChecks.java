package com.runecraft.selftest;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.Mechanics;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.CombatHooks;
import com.runecraft.combat.AttackTimer;
import com.runecraft.combat.FreezeRules;
import com.runecraft.combat.LateProtection;
import com.runecraft.combat.PrayerService;
import com.runecraft.content.systems.SysState;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.network.Net;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.ConsumeRules;
import com.runecraft.osrs.Experience;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.PrayerGrace;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.minigames.Gauntlet;
import com.runecraft.osrs.content.raids.ToaSupplies;
import com.runecraft.osrs.content.raids.TobRules;
import com.runecraft.osrs.data.Monster;
import com.runecraft.platform.UseItemCallback;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.registry.ModEntities;
import com.runecraft.skill.ConsumeCooldowns;
import com.runecraft.util.Inv;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;

/**
 * Round 41 prayer-feedback checks: the idempotent prayer set and quick-prayers, the protection switch grace through
 * {@link CombatHandler#isProtected}, the BLOCKED hitsplat, the shared food/potion delays and freeze immunity, plus one
 * boss check (works for the real player) that switches prayer through {@link PrayerService#set} some ticks after a
 * telegraph cue and asserts the blocked splat through {@link ServerNetworking#LAST_SPLAT}, not health. Round 42: the late
 * side of the grace ({@link LateProtection}: a protection switched on 3 MC after the impact blocks), raid and minigame
 * supplies on the shared delays (and food's attack delay), and a freeze broken early giving immunity and held by the server.
 */
final class PrayerFeedbackChecks {
    /** Monster attacks per window in the grace checks. */
    private static final int ROLLS_PER_WINDOW = 30;

    private final TestContext c;

    /** Damage the monster rolled against {@link #watching} (CombatHooks damage listener, registered once). */
    private static final List<Integer> ROLLS = new ArrayList<>();
    private static ServerPlayer watching;
    /** The mob whose accuracy the checks scale ({@link #accuracy}), or null. */
    private static LivingEntity tuned;
    private static double accuracy = 1.0;
    private static boolean hooked;

    private PrayerFeedbackChecks(TestContext c) {
        this.c = c;
    }

    static List<SelfTest.Check> all(TestContext context) {
        if (!hooked) {
            hooked = true;
            CombatHooks.addDamageListener((player, attacker, attack, style, damage) -> {
                if (player == watching) {
                    ROLLS.add(damage);
                }
            });
            CombatHooks.addNpcAccuracyModifier((mob, type) -> mob == tuned ? accuracy : 1.0);
        }
        PrayerFeedbackChecks g = new PrayerFeedbackChecks(context);
        List<SelfTest.Check> checks = new ArrayList<>();
        checks.add(g.check("prayer:set_idempotent", g::setIdempotent));
        checks.add(g.check("prayer:switch_grace", g::switchGrace));
        checks.add(g.check("prayer:quick_prayers", g::quickPrayers));
        checks.add(g.check("feedback:blocked_splat", g::blockedSplat));
        checks.add(g.check("feedback:shared_food_delay", g::sharedFoodDelay));
        checks.add(g.check("feedback:freeze_immunity", g::freezeImmunity));
        checks.add(g.check("boss:playability_prayer_switch", g::bossPrayerSwitch));
        // round 42: the late side of the switch grace, raid/minigame supplies on the shared delays, freezes on the server
        checks.add(g.check("prayer:late_switch", g::lateSwitch));
        // the same check under the boss: prefix, which the client smoke test's real-player run includes
        checks.add(g.check("boss:playability_late_switch", g::lateSwitch));
        checks.add(g.check("feedback:shared_supply_delay", g::sharedSupplyDelay));
        checks.add(g.check("feedback:broken_freeze_immunity", g::brokenFreezeImmunity));
        return checks;
    }

    private SelfTest.Check check(String name, SelfTest.Body body) {
        return new SelfTest.Check(name, () -> {
            fresh();
            try {
                return body.run();
            } finally {
                fresh();
            }
        });
    }

    private void fresh() {
        watching = null;
        tuned = null;
        accuracy = 1.0;
        ROLLS.clear();
        data().activePrayers().clear();
        p().removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        FreezeRules.forget(p());
    }

    private ServerPlayer p() {
        return c.player;
    }

    private PlayerData data() {
        return Players.data(c.player);
    }

    private static AssertionError failure(String message) {
        return new AssertionError(message);
    }

    private static void advance(int mcTicks) {
        for (int i = 0; i < mcTicks; i++) {
            ActionManager.advanceForSelfTest();
        }
    }

    private void setLevel(Skill skill, int level) {
        data().skills().setXpTenths(skill, Experience.xpForLevel(level) * 10);
    }

    private LivingEntity spawn(String id, int dx, int dz) {
        EntityType<?> type = ModEntities.typeById(id);
        if (type == null) {
            throw failure("no entity type " + id);
        }
        Entity e = type.create(c.world);
        if (!(e instanceof LivingEntity living)) {
            throw failure(id + " is not a living entity");
        }
        BlockPos pos = c.near(dx, dz);
        living.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
        if (living instanceof Mob mob) {
            mob.finalizeSpawn(c.world, c.world.getCurrentDifficultyAt(pos), MobSpawnType.SPAWNER, null);
            mob.setPersistenceRequired();
        }
        c.world.addFreshEntity(living);
        return living;
    }

    // ------------------------------------------------------------------ prayers

    /** set() twice leaves the prayer on once; set(false) turns it off; redirects apply; a level too low is refused. */
    private String setIdempotent() {
        PlayerData data = data();
        Prayer magic = Prayer.PROTECT_FROM_MAGIC;
        PrayerService.set(p(), magic, true);
        PrayerService.set(p(), magic, true);
        if (!data.isActive(magic) || data.activePrayers().size() != 1) {
            throw failure("two set(on) left " + data.activePrayers());
        }
        long onAt = data.overheadOnAt[0];
        PrayerService.set(p(), magic, true);
        if (data.overheadOnAt[0] != onAt) {
            throw failure("a set(on) that changed nothing restamped the prayer");
        }
        PrayerService.set(p(), magic, false);
        PrayerService.set(p(), magic, false);
        if (data.isActive(magic) || !data.activePrayers().isEmpty()) {
            throw failure("set(off) left " + data.activePrayers());
        }
        // A redirect (the Nightmare's curse) turns the click into another prayer.
        BiFunction<ServerPlayer, Prayer, Prayer> curse = (pl, pr) -> pl == p() && pr == magic ? Prayer.PROTECT_FROM_MISSILES : pr;
        PrayerService.REDIRECTS.add(curse);
        try {
            PrayerService.set(p(), magic, true);
            if (!data.isActive(Prayer.PROTECT_FROM_MISSILES) || data.isActive(magic)) {
                throw failure("a redirect Magic -> Missiles left " + data.activePrayers());
            }
            // Every cursed click toggles the target: Magic reads off, so the client sends on = true; Missiles goes off.
            PrayerService.set(p(), magic, true);
            if (!data.activePrayers().isEmpty()) {
                throw failure("a second cursed click on Magic didn't turn Missiles off: " + data.activePrayers());
            }
        } finally {
            PrayerService.REDIRECTS.remove(curse);
        }
        // Magic on before the curse: the cursed click reads "Magic off" (on = false) and still turns Missiles on.
        PrayerService.set(p(), magic, true);
        PrayerService.REDIRECTS.add(curse);
        try {
            PrayerService.set(p(), magic, false);
            if (!data.isActive(Prayer.PROTECT_FROM_MISSILES) || data.isActive(magic)) {
                throw failure("a cursed click on an active Magic left " + data.activePrayers());
            }
        } finally {
            PrayerService.REDIRECTS.remove(curse);
        }
        data.activePrayers().clear();
        // Below level 37: the level message and no change.
        setLevel(Skill.PRAYER, 30);
        data.setPrayerPoints(30);
        PrayerService.lastRefusal = null;
        PrayerService.set(p(), magic, true);
        if (data.isActive(magic)) {
            throw failure("Protect from Magic switched on at Prayer level 30");
        }
        String msg = PrayerService.lastRefusal;
        if (msg == null || !msg.contains("Prayer level of 37")) {
            throw failure("no level message (got " + msg + ")");
        }
        return "idempotent, redirected clicks toggle, level refused: " + msg;
    }

    /**
     * Protect from Melee on for 20 MC, then set(Magic) at T: melee is still blocked at T+4 (grace) but gets through at
     * T+10; a forced removal gives no grace.
     */
    private String switchGrace() {
        c.completeQuests(null);
        setLevel(Skill.DEFENCE, 1);
        LivingEntity mob = spawn(Monster.BLACK_DEMON.entity(), 2, 0);
        tuned = mob;
        accuracy = 50.0; // a sure hit, so a 0 means the prayer took it
        watching = p();
        PlayerData data = data();
        PrayerService.set(p(), Prayer.PROTECT_FROM_MELEE, true);
        advance(20);
        PrayerService.set(p(), Prayer.PROTECT_FROM_MAGIC, true);
        if (data.isActive(Prayer.PROTECT_FROM_MELEE) || !data.isActive(Prayer.PROTECT_FROM_MAGIC)) {
            throw failure("switching to Magic left " + data.activePrayers());
        }
        long t = ActionManager.mcTick();
        advance(4);
        int[] graced = rolls(mob, AttackType.SLASH);
        if (graced[0] == 0) {
            throw new SelfTest.Skip("no monster attack reached the damage listener (CombatZones refused?)");
        }
        if (graced[1] > 0) {
            throw failure(graced[1] + " of " + graced[0] + " melee hits got through 4 MC after the switch (grace is 8)");
        }
        if (!CombatHandler.isProtected(data, AttackType.CRUSH) || CombatHandler.isProtected(data, AttackType.RANGED)) {
            throw failure("isProtected at T+4: melee " + CombatHandler.isProtected(data, AttackType.CRUSH)
                    + ", ranged " + CombatHandler.isProtected(data, AttackType.RANGED));
        }
        advance((int) (t + 10 - ActionManager.mcTick()));
        int[] late = rolls(mob, AttackType.SLASH);
        if (late[0] == 0) {
            throw new SelfTest.Skip("the monster stopped attacking (CombatZones refused?)");
        }
        if (late[1] == 0) {
            throw failure("no melee hit got through 10 MC after the switch (" + late[0] + " rolls)");
        }
        // A forced removal (drain, boss disable) leaves no grace.
        PrayerService.set(p(), Prayer.PROTECT_FROM_MELEE, true);
        advance(20);
        data.activePrayers().remove(Prayer.PROTECT_FROM_MELEE);
        int[] forced = rolls(mob, AttackType.SLASH);
        if (forced[1] == 0) {
            throw failure("melee was still blocked right after a forced removal (" + forced[0] + " rolls)");
        }
        // Too short on: switched off after 6 MC, no grace.
        PrayerService.set(p(), Prayer.PROTECT_FROM_MELEE, true);
        advance(6);
        PrayerService.set(p(), Prayer.PROTECT_FROM_MELEE, false);
        advance(2);
        if (CombatHandler.isProtected(data, AttackType.STAB)) {
            throw failure("a prayer on for only 6 MC got the switch grace");
        }
        return "T+4: " + graced[0] + " rolls all 0; T+10: " + late[1] + "/" + late[0] + " hit; forced removal: "
                + forced[1] + "/" + forced[0] + " hit";
    }

    /** {@link #ROLLS_PER_WINDOW} monster attacks now; returns {rolls seen, rolls that did damage}. */
    private int[] rolls(LivingEntity mob, AttackType type) {
        ROLLS.clear();
        for (int i = 0; i < ROLLS_PER_WINDOW; i++) {
            CombatHandler.clearCooldown(mob);
            CombatHandler.npcAttack(mob, p(), type);
            p().setHealth(p().getMaxHealth());
        }
        int hit = 0;
        for (int d : ROLLS) {
            if (d > 0) {
                hit++;
            }
        }
        int n = ROLLS.size();
        ROLLS.clear();
        return new int[]{n, hit};
    }

    /** toggleQuick turns {Piety, Protect from Melee} on, then off; the selection survives a save. */
    private String quickPrayers() {
        c.completeQuests(null);
        SysState.of(p()).prayerScrolls.add(Prayer.PIETY.name());
        PlayerData data = data();
        PrayerService.lastRefusal = null;
        PrayerService.toggleQuick(p());
        if (PrayerService.lastRefusal != null || !data.activePrayers().isEmpty()) {
            throw failure("an empty quick-prayer set switched something");
        }
        long mask = PrayerGrace.mask(List.of(Prayer.PIETY, Prayer.PROTECT_FROM_MELEE));
        PrayerService.setQuickPrayers(p(), mask);
        PrayerService.toggleQuick(p());
        if (!data.isActive(Prayer.PIETY) || !data.isActive(Prayer.PROTECT_FROM_MELEE) || data.activePrayers().size() != 2) {
            throw failure("quick-prayers on gave " + data.activePrayers() + (PrayerService.lastRefusal == null ? ""
                    : " (" + PrayerService.lastRefusal + ")"));
        }
        PrayerService.toggleQuick(p());
        if (!data.activePrayers().isEmpty()) {
            throw failure("quick-prayers off left " + data.activePrayers());
        }
        // Cursed (Melee -> Magic): the key turns on what a click would, Piety and Protect from Magic, then turns both off.
        BiFunction<ServerPlayer, Prayer, Prayer> curse = (pl, pr) -> pl == p() && pr == Prayer.PROTECT_FROM_MELEE
                ? Prayer.PROTECT_FROM_MAGIC : pr;
        PrayerService.REDIRECTS.add(curse);
        try {
            PrayerService.toggleQuick(p());
            if (!data.isActive(Prayer.PIETY) || !data.isActive(Prayer.PROTECT_FROM_MAGIC) || data.activePrayers().size() != 2) {
                throw failure("cursed quick-prayers on gave " + data.activePrayers());
            }
            PrayerService.toggleQuick(p());
            if (!data.activePrayers().isEmpty()) {
                throw failure("cursed quick-prayers off left " + data.activePrayers());
            }
        } finally {
            PrayerService.REDIRECTS.remove(curse);
        }
        PlayerData copy = new PlayerData();
        copy.fromNbt(data.toNbt(), c.server.registryAccess());
        if (copy.quickPrayers() != mask) {
            throw failure("the quick-prayer selection didn't survive a save: " + copy.quickPrayers() + " != " + mask);
        }
        return "on and off (also cursed), saved as QuickPrayers";
    }

    // ------------------------------------------------------------------ feedback

    /** A prayed monster hit sends SPLAT_BLOCKED naming the monster; an unprayed 0 still sends SPLAT_MISS. */
    private String blockedSplat() {
        LivingEntity mob = spawn(Monster.BLACK_DEMON.entity(), 2, 0);
        tuned = mob;
        accuracy = 50.0;
        PrayerService.set(p(), Prayer.PROTECT_FROM_MELEE, true);
        ServerNetworking.LAST_SPLAT = null;
        CombatHandler.clearCooldown(mob);
        CombatHandler.npcAttack(mob, p(), AttackType.CRUSH);
        ServerNetworking.Splat blocked = ServerNetworking.LAST_SPLAT;
        if (blocked == null) {
            throw new SelfTest.Skip("the monster's attack never resolved (CombatZones refused?)");
        }
        if (blocked.target() != p().getId() || blocked.type() != Net.SPLAT_BLOCKED || blocked.attacker() != mob.getId()) {
            throw failure("a prayed hit sent " + blocked + ", expected SPLAT_BLOCKED from " + mob.getId());
        }
        PrayerService.set(p(), Prayer.PROTECT_FROM_MELEE, false);
        advance(PrayerGrace.GRACE_MC + 1);
        accuracy = 0.0; // a sure miss
        ServerNetworking.LAST_SPLAT = null;
        CombatHandler.clearCooldown(mob);
        CombatHandler.npcAttack(mob, p(), AttackType.CRUSH);
        p().setHealth(p().getMaxHealth());
        ServerNetworking.Splat miss = ServerNetworking.LAST_SPLAT;
        if (miss == null || miss.type() != Net.SPLAT_MISS || miss.attacker() != mob.getId()) {
            throw failure("an unprayed miss sent " + miss + ", expected SPLAT_MISS");
        }
        return "blocked and miss splats name attacker " + mob.getId();
    }

    /** Shark then manta on one tick: the manta is refused; a karambwan and a potion are accepted; the manta after 36 MC. */
    private String sharedFoodDelay() {
        p().getInventory().clearContent();
        p().getInventory().setItem(1, Inv.stack("shark", 2));
        p().getInventory().setItem(2, Inv.stack("manta_ray", 2));
        p().getInventory().setItem(3, Inv.stack("cooked_karambwan", 2));
        p().getInventory().setItem(4, Inv.stack("prayer_potion", 1));
        for (int i = 1; i <= 4; i++) {
            if (p().getInventory().getItem(i).isEmpty()) {
                throw failure("item in slot " + i + " is not registered");
            }
            p().getCooldowns().removeCooldown(p().getInventory().getItem(i).getItem());
        }
        p().getInventory().selected = 0;
        p().setHealth(10f);
        if (!consume(1)) {
            throw failure("the first shark was not eaten");
        }
        if (!p().getCooldowns().isOnCooldown(p().getInventory().getItem(2).getItem())) {
            throw failure("eating a shark didn't show the shared eat delay on the manta rays");
        }
        if (p().getCooldowns().isOnCooldown(p().getInventory().getItem(3).getItem())) {
            throw failure("eating a shark put the karambwan (combo food) on cooldown");
        }
        if (consume(2)) {
            throw failure("a manta ray was eaten on the same tick as a shark (OSRS: one 3-tick eat delay for all food)");
        }
        if (!consume(3)) {
            throw failure("a cooked karambwan was refused right after a shark (OSRS combo eating)");
        }
        if (consume(3)) {
            throw failure("a second karambwan followed the first on the same tick");
        }
        if (!consume(4)) {
            throw failure("a potion was refused right after food (OSRS: separate potion delay)");
        }
        if (consume(4)) {
            throw failure("a second potion dose went down on the same tick");
        }
        for (int i = 0; i < ConsumeCooldowns.FOOD_MC; i++) {
            ActionManager.advanceForSelfTest();
            p().getCooldowns().tick();
        }
        if (!consume(2)) {
            throw failure("the manta ray was still refused " + ConsumeCooldowns.FOOD_MC + " MC later");
        }
        for (int i = 0; i < ConsumeCooldowns.FOOD_MC; i++) {
            ActionManager.advanceForSelfTest();
            p().getCooldowns().tick();
        }
        return "shark + karambwan + potion on one tick, manta refused until 36 MC";
    }

    /** Uses the stack in inventory slot {@code slot} as a right-click; returns whether it was consumed. */
    private boolean consume(int slot) {
        ItemStack stack = p().getInventory().getItem(slot);
        if (stack.isEmpty()) {
            throw failure("slot " + slot + " is empty");
        }
        String id = Inv.idOf(stack);
        int count = stack.getCount();
        int dose = stack.getDamageValue();
        p().getInventory().setItem(0, stack);
        p().getInventory().setItem(slot, ItemStack.EMPTY);
        InteractionResultHolder<ItemStack> result = UseItemCallback.EVENT.invoker().interact(p(), c.world, InteractionHand.MAIN_HAND);
        if (result.getResult() == InteractionResult.PASS) {
            ItemStack held = p().getMainHandItem();
            if (!held.isEmpty()) {
                held.getItem().use(c.world, p(), InteractionHand.MAIN_HAND);
            }
        }
        ItemStack after = p().getInventory().getItem(0);
        p().getInventory().setItem(slot, after);
        p().getInventory().setItem(0, ItemStack.EMPTY);
        return after.isEmpty() || !Inv.idOf(after).equals(id) || after.getCount() < count || after.getDamageValue() > dose;
    }

    /** A second freeze 2 game ticks after one runs out is refused; 6 ticks after it lands; run-energy Slowness II always. */
    private String freezeImmunity() {
        FreezeRules.forget(p());
        p().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 3, 10, false, true));
        if (!p().hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
            throw failure("the first freeze didn't land");
        }
        // Let it run out on its own (the fake player is never ticked, so tick its effects here).
        for (int i = 0; i < 10 && p().hasEffect(MobEffects.MOVEMENT_SLOWDOWN); i++) {
            p().baseTick();
        }
        if (p().hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
            throw new SelfTest.Skip("the freeze didn't run out after 10 effect ticks");
        }
        advance(2 * ActionManager.MC_TICKS_PER_GAME_TICK);
        if (p().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 10, false, true))
                || p().hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
            throw failure("a freeze landed 2 game ticks after the last one ran out (OSRS: 5 ticks of immunity)");
        }
        // Run-energy Slowness II is not a freeze.
        if (!p().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, false, false))) {
            throw failure("run-energy Slowness II was refused during freeze immunity");
        }
        p().removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        advance(4 * ActionManager.MC_TICKS_PER_GAME_TICK);
        if (!p().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 10, false, true))) {
            throw failure("a freeze was refused 6 game ticks after the last one ran out");
        }
        p().removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        return "immune for " + FreezeRules.IMMUNITY_MC + " MC after a freeze runs out";
    }

    /**
     * A telegraphed magic attack; the player switches to Protect from Magic through the real set path 8 MC after the cue,
     * and the hit resolves as a BLOCKED splat naming the monster (asserted through the splat recorder, not health, so it
     * also holds for the invulnerable real player).
     */
    private String bossPrayerSwitch() {
        LivingEntity spawned = spawn(Monster.BLACK_DEMON.entity(), 3, 0);
        if (!(spawned instanceof OsrsMonsterEntity mob)) {
            throw new SelfTest.Skip("the black demon isn't an OSRS monster entity here");
        }
        AttackDef blast = AttackDef.of("playability_test_blast", AttackType.MAGIC, 30, 20, 1)
                .telegraph(3, "The demon gathers dark magic!");
        PrayerService.set(p(), Prayer.PROTECT_FROM_MELEE, true); // the wrong prayer when the cue appears
        ServerNetworking.LAST_SPLAT = null;
        long cue = ActionManager.mcTick();
        Mechanics.perform(mob, p(), blast, 1.0);
        ServerNetworking.Splat landed = null;
        for (int i = 1; i <= 240 && landed == null; i++) {
            ActionManager.advanceForSelfTest();
            if (i == 8) {
                PrayerService.set(p(), Prayer.PROTECT_FROM_MAGIC, true);
            }
            Mechanics.tickForSelfTest();
            ServerNetworking.Splat s = ServerNetworking.LAST_SPLAT;
            if (s != null && s.target() == p().getId() && s.mcTick() > cue) {
                landed = s;
            }
            c.heal();
        }
        if (landed == null) {
            throw failure("the telegraphed blast never landed within 240 MC");
        }
        long lead = landed.mcTick() - cue;
        if (lead <= 8) {
            throw failure("the blast landed " + lead + " MC after its cue, before the prayer switch at 8 MC");
        }
        if (landed.type() != Net.SPLAT_BLOCKED || landed.attacker() != mob.getId()) {
            throw failure("the blast after a switch to Protect from Magic sent " + landed + ", expected SPLAT_BLOCKED from "
                    + mob.getId());
        }
        return "switched 8 MC after the cue, blocked " + lead + " MC after it";
    }

    // ------------------------------------------------------------------ round 42: late window, shared supplies, freezes

    /** One Minecraft tick of the checks' world: the clock, Mechanics' pending hits and the held hits. */
    private static void tickOne() {
        ActionManager.advanceForSelfTest();
        Mechanics.tickForSelfTest();
    }

    /**
     * The late side of the switch grace: a monster hit landing while Protect from Missiles is up (the wrong prayer) waits, and
     * Protect from Melee switched on 3 MC after the impact blocks it (a BLOCKED splat), on both landing paths (a delivered
     * built-in hit through CombatHandler.npcAttack, and a Mechanics AttackDef swing); switched on 6 MC after, the hit has landed
     * unchanged at the window's end; a player praying nothing is never held. Asserted through LateProtection's listener and the
     * splat recorder, not health, so it holds for the real player too.
     */
    private String lateSwitch() {
        c.completeQuests(null);
        setLevel(Skill.DEFENCE, 1);
        LivingEntity spawned = spawn(Monster.BLACK_DEMON.entity(), 2, 0);
        if (!(spawned instanceof OsrsMonsterEntity mob)) {
            throw new SelfTest.Skip("the black demon isn't an OSRS monster entity here");
        }
        tuned = mob;
        accuracy = 50.0; // a sure hit, so a 0 means the prayer took it
        List<long[]> resolved = new ArrayList<>(); // {heldAt, resolvedAt, blocked}
        LateProtection.Listener before = LateProtection.listener;
        LateProtection.listener = new LateProtection.Listener() {
            @Override
            public void held(ServerPlayer target, AttackType style, long mc) {
                before.held(target, style, mc);
            }

            @Override
            public void resolved(ServerPlayer target, AttackType style, long heldAt, long mc, boolean blocked) {
                before.resolved(target, style, heldAt, mc, blocked);
                if (target == p()) {
                    resolved.add(new long[]{heldAt, mc, blocked ? 1 : 0});
                }
            }
        };
        try {
            // 1. a delivered built-in hit (its wind-up or flight already over), Melee switched on 3 MC late
            for (int i = 0; i < 4; i++) {
                wrongPrayer();
                resolved.clear();
                ServerNetworking.LAST_SPLAT = null;
                long impact = ActionManager.mcTick();
                CombatHandler.npcAttack(mob, p(), AttackType.SLASH, true);
                if (LateProtection.heldOn(p()) == 0) {
                    if (ServerNetworking.LAST_SPLAT == null) {
                        throw new SelfTest.Skip("the monster's attack never resolved (CombatZones refused?)");
                    }
                    throw failure("a melee hit on a player praying Protect from Missiles landed at once (no late window)");
                }
                lateMelee(3);
                expectBlocked(resolved, impact, 3, mob, "a delivered hit");
            }
            // 2. a Mechanics AttackDef swing after its wind-up, the same switch
            AttackDef swing = AttackDef.of("late_switch_test_swing", AttackType.SLASH, 30, 2, 1);
            for (int i = 0; i < 2; i++) {
                wrongPrayer();
                resolved.clear();
                ServerNetworking.LAST_SPLAT = null;
                Mechanics.perform(mob, p(), swing, 1.0);
                long impact = -1;
                for (int t = 0; t < 80 && impact < 0; t++) {
                    tickOne();
                    topUp();
                    if (LateProtection.heldOn(p()) > 0) {
                        impact = ActionManager.mcTick();
                    }
                }
                if (impact < 0) {
                    throw failure("the swing never landed on a player in reach with the wrong prayer up (80 MC; splat "
                            + ServerNetworking.LAST_SPLAT + ")");
                }
                lateMelee(3);
                expectBlocked(resolved, impact, 3, mob, "a Mechanics swing");
            }
            // 3. 6 MC late is too late: the hit landed unchanged when the window ended, 4 MC after the impact
            int hurt = 0;
            for (int i = 0; i < 6; i++) {
                wrongPrayer();
                resolved.clear();
                ServerNetworking.LAST_SPLAT = null;
                long impact = ActionManager.mcTick();
                CombatHandler.npcAttack(mob, p(), AttackType.SLASH, true);
                lateMelee(6);
                if (resolved.size() != 1 || resolved.get(0)[1] != impact + PrayerGrace.LATE_MC || resolved.get(0)[2] != 0) {
                    throw failure("a switch 6 MC late: resolved " + describe(resolved, impact) + ", expected unblocked at +"
                            + PrayerGrace.LATE_MC);
                }
                ServerNetworking.Splat s = ServerNetworking.LAST_SPLAT;
                if (s != null && s.type() == Net.SPLAT_DAMAGE) {
                    hurt++;
                }
            }
            if (hurt == 0) {
                throw failure("none of 6 hits a switch 6 MC late missed did damage (a sure-hit monster)");
            }
            // 4. praying nothing (and no switch for two game ticks): the hit lands on its tick
            data().activePrayers().clear();
            advance(PrayerGrace.SWITCHING_MC + 1);
            ServerNetworking.LAST_SPLAT = null;
            CombatHandler.npcAttack(mob, p(), AttackType.SLASH, true);
            if (LateProtection.heldOn(p()) != 0 || ServerNetworking.LAST_SPLAT == null) {
                throw failure("a hit on a player praying nothing was held (" + LateProtection.heldOn(p()) + " held)");
            }
            topUp();
            return "Melee 3 MC after the impact: 6/6 blocked (delivered hits and Mechanics swings); 6 MC after: landed at +"
                    + PrayerGrace.LATE_MC + " (" + hurt + "/6 hurt); praying nothing: not held"
                    + (c.realPlayer() ? " (real player)" : "");
        } finally {
            LateProtection.listener = before;
            LateProtection.flush(p());
            topUp();
        }
    }

    /** Back to full health (the fake player isn't invulnerable; the real one is healed the same way). */
    private void topUp() {
        p().setHealth(p().getMaxHealth());
    }

    /** Protect from Missiles up (the wrong prayer for a melee hit) and any melee grace from the last round gone. */
    private void wrongPrayer() {
        PrayerService.set(p(), Prayer.PROTECT_FROM_MISSILES, true);
        advance(PrayerGrace.GRACE_MC + 1);
        if (!data().isActive(Prayer.PROTECT_FROM_MISSILES) || CombatHandler.isProtected(data(), AttackType.SLASH)) {
            throw failure("couldn't set up the wrong prayer: " + data().activePrayers());
        }
    }

    /** {@code late} MC ticks, switching to Protect from Melee through the real set path on the last one. */
    private void lateMelee(int late) {
        for (int t = 1; t <= late; t++) {
            ActionManager.advanceForSelfTest();
            if (t == late) {
                PrayerService.set(p(), Prayer.PROTECT_FROM_MELEE, true);
            }
            Mechanics.tickForSelfTest();
            topUp();
        }
    }

    private void expectBlocked(List<long[]> resolved, long impact, int late, LivingEntity mob, String what) {
        if (resolved.size() != 1 || resolved.get(0)[0] != impact || resolved.get(0)[1] != impact + late || resolved.get(0)[2] != 1) {
            throw failure(what + " with Melee switched on " + late + " MC after the impact: " + describe(resolved, impact));
        }
        ServerNetworking.Splat s = ServerNetworking.LAST_SPLAT;
        if (s == null || s.type() != Net.SPLAT_BLOCKED || s.attacker() != mob.getId()) {
            throw failure(what + " blocked " + late + " MC late sent " + s + ", expected SPLAT_BLOCKED from " + mob.getId());
        }
    }

    private static String describe(List<long[]> resolved, long impact) {
        StringBuilder b = new StringBuilder();
        for (long[] r : resolved) {
            b.append(b.length() == 0 ? "" : ", ").append("held at +").append(r[0] - impact).append(", resolved at +")
                    .append(r[1] - impact).append(r[2] == 1 ? " blocked" : " unblocked");
        }
        return resolved.isEmpty() ? "nothing resolved" : b.toString();
    }

    /**
     * Raid and minigame supplies on the shared timers: a Theatre of Blood bandage (raid food) then a Gauntlet paddlefish
     * (minigame food) on one tick, the paddlefish is refused; an egniol potion (minigame potion) right after is accepted
     * (separate potion delay), and holds a prayer potion and nectar (a Tombs of Amascut potion); the paddlefish goes down
     * 36 MC later. The bandage, being food, adds 3 ticks to a weapon delay still running; the potion adds nothing; with the
     * weapon ready, eating adds nothing (wiki Food/Fast foods).
     */
    private String sharedSupplyDelay() {
        p().getInventory().clearContent();
        String[] ids = {TobRules.BANDAGES, Gauntlet.PADDLEFISH, Gauntlet.EGNIOL, "prayer_potion", ToaSupplies.NECTAR, "shark"};
        for (int i = 0; i < ids.length; i++) {
            p().getInventory().setItem(i + 1, Inv.stack(ids[i], 2));
            if (p().getInventory().getItem(i + 1).isEmpty()) {
                throw failure(ids[i] + " is not registered");
            }
            p().getCooldowns().removeCooldown(p().getInventory().getItem(i + 1).getItem());
        }
        p().getInventory().selected = 0;
        p().setHealth(10f);
        AttackTimer.forget(p().getUUID());
        data().lastAttackTick = ActionManager.mcTick(); // the weapon is recovering from an attack just now
        long ready0 = AttackTimer.readyAt(p());
        if (!consume(1)) {
            throw failure("the Theatre of Blood bandage was not used");
        }
        long ready1 = AttackTimer.readyAt(p());
        if (ready1 - ready0 != ConsumeRules.FOOD_ATTACK_MC) {
            throw failure("a bandage (food) moved the next attack by " + (ready1 - ready0) + " MC, expected "
                    + ConsumeRules.FOOD_ATTACK_MC);
        }
        if (consume(2)) {
            throw failure("a paddlefish was eaten on the same tick as a bandage (OSRS: one 3-tick eat delay for all food)");
        }
        if (!consume(3)) {
            throw failure("an egniol potion was refused right after food (OSRS: a separate potion delay)");
        }
        if (AttackTimer.readyAt(p()) != ready1) {
            throw failure("a potion moved the next attack (OSRS: potions don't delay attacks)");
        }
        if (consume(4)) {
            throw failure("a prayer potion went down on the same tick as an egniol potion");
        }
        if (!ConsumeCooldowns.blocked(p(), p().getInventory().getItem(5))) {
            throw failure("nectar (a Tombs of Amascut potion) isn't held by the potion delay an egniol potion started");
        }
        for (int i = 0; i < ConsumeRules.FOOD_MC; i++) {
            ActionManager.advanceForSelfTest();
            p().getCooldowns().tick();
        }
        if (!consume(2)) {
            throw failure("the paddlefish was still refused " + ConsumeRules.FOOD_MC + " MC later");
        }
        // weapon ready: eating adds no delay
        AttackTimer.forget(p().getUUID());
        data().lastAttackTick = -1_000_000;
        for (int i = 0; i < ConsumeRules.FOOD_MC; i++) {
            ActionManager.advanceForSelfTest();
            p().getCooldowns().tick();
        }
        if (!consume(6)) {
            throw failure("the shark was refused " + ConsumeRules.FOOD_MC + " MC after the paddlefish");
        }
        if (!AttackTimer.ready(p())) {
            throw failure("eating with the weapon ready delayed the next attack");
        }
        for (int i = 0; i < ConsumeRules.FOOD_MC; i++) {
            ActionManager.advanceForSelfTest();
            p().getCooldowns().tick();
        }
        return "bandage + egniol on one tick, paddlefish refused until 36 MC, prayer potion and nectar held by the egniol; "
                + "the bandage added " + (ready1 - ready0) + " MC to a running weapon delay";
    }

    /**
     * A freeze broken early (a cure) starts the 5-tick immunity too, and the server holds a frozen player: their own moves
     * (simulated with {@code setPos}, which no teleport reports) are put back however far, 0.8 or 3 blocks, at the held
     * height after a drop (never inside the floor under the held spot) and at their own after a freeze caught them mid-jump;
     * a server teleport of 3 blocks (OSRS allows teleporting while frozen; reported by the teleport mixin) holds them at the
     * new spot.
     */
    private String brokenFreezeImmunity() {
        FreezeRules.forget(p());
        c.home();
        p().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 10, false, true));
        if (!FreezeRules.frozen(p())) {
            throw new SelfTest.Skip("the freeze didn't take (outside Gielinor?)");
        }
        FreezeRules.enforce(p()); // held here
        net.minecraft.world.phys.Vec3 at = p().position();
        p().setPos(at.x + 0.8, at.y, at.z);
        if (!FreezeRules.enforce(p()) || Math.abs(p().getX() - at.x) > 0.01 || Math.abs(p().getZ() - at.z) > 0.01) {
            throw failure("a frozen player who walked 0.8 blocks stayed at " + p().position() + " (held at " + at + ")");
        }
        // a client that ignores the freeze steps 3 blocks in one tick: no teleport, so it is put back too
        p().setPos(at.x + 3, at.y, at.z);
        if (!FreezeRules.enforce(p()) || Math.abs(p().getX() - at.x) > 0.01) {
            throw failure("a frozen player who stepped 3 blocks on their own (no server teleport) was left at " + p().position()
                    + " (held at " + at + ")");
        }
        // slid 0.6 blocks off the held spot and dropped a block: over the held spot at that height is the floor itself
        p().setPos(at.x + 0.6, at.y - 1, at.z);
        if (!FreezeRules.enforce(p()) || Math.abs(p().getY() - at.y) > 0.01 || Math.abs(p().getX() - at.x) > 0.01) {
            throw failure("a frozen player who dropped a block off the held spot was put back at " + p().position()
                    + " (held at " + at + "; their own height is inside the floor)");
        }
        // caught mid-jump, then landed and walked off: put back on the floor, not lifted into the air
        FreezeRules.forget(p());
        p().setPos(at.x, at.y + 1, at.z);
        FreezeRules.enforce(p()); // held in the air
        p().setPos(at.x + 0.8, at.y, at.z);
        if (!FreezeRules.enforce(p()) || Math.abs(p().getY() - at.y) > 0.01 || Math.abs(p().getX() - at.x) > 0.01) {
            throw failure("a frozen player caught mid-jump who landed and walked off was put back at " + p().position()
                    + " (expected the floor under the held spot, " + at + ")");
        }
        // a server teleport while frozen (OSRS allows it): held at the new spot
        p().teleportTo(at.x + 3, at.y, at.z);
        if (FreezeRules.enforce(p()) || Math.abs(p().getX() - (at.x + 3)) > 0.01) {
            throw failure("a frozen player teleported 3 blocks was put back to " + p().position()
                    + " (the teleport mixin didn't report the server's move?)");
        }
        p().setPos(at.x + 3.6, at.y, at.z);
        if (!FreezeRules.enforce(p()) || Math.abs(p().getX() - (at.x + 3)) > 0.01) {
            throw failure("after the teleport the freeze didn't hold the player at the new spot (" + p().position() + ")");
        }
        c.home();
        // broken early by a cure: the immunity starts now
        p().removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        advance(2 * ActionManager.MC_TICKS_PER_GAME_TICK);
        if (p().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 10, false, true))
                || p().hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
            throw failure("a freeze landed 2 game ticks after one was broken early (OSRS: 5 ticks of immunity after any freeze ends)");
        }
        advance(4 * ActionManager.MC_TICKS_PER_GAME_TICK);
        if (!p().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 10, false, true))) {
            throw failure("a freeze was refused 6 game ticks after the last one was broken");
        }
        p().removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        FreezeRules.forget(p());
        return "held against a 0.8- and a 3-block step and a drop off the spot (back at the held height), moved by a 3-block "
                + "teleport; immune for " + FreezeRules.IMMUNITY_MC + " MC after a freeze broken early";
    }
}
