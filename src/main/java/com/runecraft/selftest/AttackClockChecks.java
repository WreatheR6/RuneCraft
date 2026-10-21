package com.runecraft.selftest;

import com.runecraft.action.ActionManager;
import com.runecraft.combat.AttackTimer;
import com.runecraft.combat.AutoRetaliate;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.CombatHooks;
import com.runecraft.combat.MagicService;
import com.runecraft.item.OsrsEquipmentItems;
import com.runecraft.osrs.AttackClock;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.CombatStyle;
import com.runecraft.osrs.Rune;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.Spell;
import com.runecraft.platform.AttackEntityCallback;
import com.runecraft.platform.UseItemCallback;
import com.runecraft.player.PlayerData;
import com.runecraft.player.PlayerEvents;
import com.runecraft.player.Players;
import com.runecraft.registry.ModEntities;
import com.runecraft.util.Inv;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * Round 41's player attack loop ({@code attack:*}): the one attack clock with its input buffer keeps the OSRS attack rate
 * exactly (attacks counted with an afterHit listener, never health), for melee, the left-click entry point, staff
 * autocasts, bows (hold to fire, taps, no durability), thrown weapons, auto-retaliate and the Nightmare's slow. Time is
 * driven like the boss checks: {@link ActionManager#advanceForSelfTest} plus {@link AttackTimer#tickForSelfTest} (or, for
 * the real player, the real per-MC-tick path {@link PlayerEvents#everyMcTick}).
 */
final class AttackClockChecks {
    private static final String DUMMY = "moss_giant";
    /** 480 MC = 24 s: exactly 10 whip attacks (speed 4 = 48 MC). */
    private static final int WINDOW_MC = 480;
    private static final int WHIP_PERIOD = 48;

    /** MC ticks of the watched player's resolved attacks (afterHit). */
    private static final List<Long> HITS = new ArrayList<>();
    private static ServerPlayer watching;
    /** The Nightmare's spores (ticks) applied on the watched player's attack tick, or 0. */
    private static int nightmareSlow;
    private static boolean hooked;

    private final TestContext c;

    private AttackClockChecks(TestContext c) {
        this.c = c;
    }

    static List<SelfTest.Check> all(TestContext context) {
        AttackClockChecks g = new AttackClockChecks(context);
        if (!hooked) {
            hooked = true;
            CombatHooks.addModifier(new CombatHooks.PlayerAttackModifier() {
                @Override
                public void afterHit(ServerPlayer player, LivingEntity target, AttackType type, int dealt) {
                    if (player != watching) {
                        return;
                    }
                    long now = ActionManager.mcTick();
                    HITS.add(now);
                    PlayerData data = Players.data(player);
                    // NightmareScript's spores: "if (data.lastAttackTick == now) data.lastAttackTick = now + slow * 12".
                    if (nightmareSlow > 0 && data.lastAttackTick == now) {
                        data.lastAttackTick = now + (long) nightmareSlow * ActionManager.MC_TICKS_PER_GAME_TICK;
                    }
                }
            });
        }
        List<SelfTest.Check> checks = new ArrayList<>();
        checks.add(g.check("attack:clock_rate", g::rate));
        checks.add(g.check("attack:clock_early_click", g::earlyClick));
        checks.add(g.check("attack:clock_slot_change", g::slotChange));
        checks.add(g.check("attack:clock_left_click", g::leftClick));
        checks.add(g.check("attack:clock_staff_autocast", g::staffAutocast));
        checks.add(g.check("attack:clock_bow", g::bow));
        checks.add(g.check("attack:clock_shared", g::shared));
        checks.add(g.check("attack:clock_retaliate", g::retaliate));
        checks.add(g.check("attack:clock_nightmare_slow", g::nightmare));
        checks.add(g.check("attack:clock_indicator", g::indicator));
        checks.add(g.check("boss:playability_attack_buffer", g::realBuffer));
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
        HITS.clear();
        nightmareSlow = 0;
        AttackTimer.forget(p().getUUID());
        AutoRetaliate.stop(p());
        p().stopUsingItem();
        com.runecraft.content.systems.SysState.of(p()).autocast = "";
        data().setAutocast(null);
        data().setStyle(CombatStyle.ACCURATE);
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

    // ------------------------------------------------------------------ helpers

    private LivingEntity dummy() {
        EntityType<?> type = ModEntities.typeById(DUMMY);
        if (type == null) {
            throw failure("no entity type " + DUMMY);
        }
        Entity e = type.create(c.world);
        if (!(e instanceof LivingEntity living)) {
            throw failure(DUMMY + " is not a living entity");
        }
        BlockPos pos = c.near(2, 0);
        living.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
        if (living instanceof Mob mob) {
            mob.finalizeSpawn(c.world, c.world.getCurrentDifficultyAt(pos), MobSpawnType.SPAWNER, null);
            mob.setPersistenceRequired();
            mob.setNoAi(true);
        }
        c.world.addFreshEntity(living);
        face(living);
        return living;
    }

    private void face(LivingEntity target) {
        p().lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
    }

    private ItemStack hold(String id, int count) {
        ItemStack s = Inv.stack(id, count);
        if (s.isEmpty()) {
            throw failure("item " + id + " is not registered");
        }
        p().setItemInHand(InteractionHand.MAIN_HAND, s);
        return p().getMainHandItem();
    }

    /** The clock ready now, nothing recorded or buffered (what the boss checks do before an attack). */
    private void ready() {
        AttackTimer.forget(p().getUUID());
        data().lastAttackTick = ActionManager.mcTick() - 10_000;
    }

    private void watch() {
        watching = p();
        HITS.clear();
    }

    /** The per-MC-tick work for the player at the current tick: the real path for a real player, else the self-test hook. */
    private void tickPlayer() {
        if (c.realPlayer()) {
            PlayerEvents.everyMcTick(c.server, ActionManager.mcTick());
        } else {
            AttackTimer.tickForSelfTest(p());
        }
    }

    /** Moves time on one MC tick (item cooldowns too: the fake player is never ticked). */
    private void advance() {
        ActionManager.advanceForSelfTest();
        p().getCooldowns().tick();
    }

    private void heal(LivingEntity mob) {
        mob.setHealth(mob.getMaxHealth());
        c.heal();
        if (!c.realPlayer()) {
            p().setHealth(p().getMaxHealth());
        }
    }

    /** Resolution ticks relative to {@code t0}. */
    private static List<Long> since(long t0) {
        List<Long> out = new ArrayList<>();
        for (long t : HITS) {
            out.add(t - t0);
        }
        return out;
    }

    private static List<Long> every(long period, int n) {
        List<Long> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(period * i);
        }
        return out;
    }

    private List<AbstractArrow> arrows() {
        AABB box = new AABB(TestContext.PLATFORM).inflate(40);
        return c.world.getEntitiesOfClass(AbstractArrow.class, box, a -> a.getOwner() == p());
    }

    private int takeArrows() {
        List<AbstractArrow> list = arrows();
        list.forEach(Entity::discard);
        return list.size();
    }

    // ------------------------------------------------------------------ checks

    /** (a) Clicking a monster on every MC tick for 480 MC with a whip: exactly 10 attacks, at 0, 48, ..., 432. */
    private String rate() {
        LivingEntity mob = dummy();
        hold("abyssal_whip", 1);
        ready();
        watch();
        long t0 = ActionManager.mcTick();
        for (int i = 0; i < WINDOW_MC; i++) {
            tickPlayer();
            AttackTimer.click(p(), mob, AttackClock.Kind.MELEE);
            heal(mob);
            advance();
        }
        List<Long> got = since(t0);
        List<Long> want = every(WHIP_PERIOD, WINDOW_MC / WHIP_PERIOD);
        if (!got.equals(want)) {
            throw failure("clicking every MC tick resolved at " + got + ", expected " + want);
        }
        return got.size() + " attacks in " + WINDOW_MC + " MC, exactly the OSRS rate (1 per 2.4 s)";
    }

    /** (b) A click 8 MC early is buffered and resolves exactly on the ready tick, not before. */
    private String earlyClick() {
        LivingEntity mob = dummy();
        hold("abyssal_whip", 1);
        ready();
        watch();
        long t0 = ActionManager.mcTick();
        if (!AttackTimer.click(p(), mob, AttackClock.Kind.MELEE)) {
            throw failure("a click on a ready clock did not resolve at once");
        }
        for (int i = 0; i < WHIP_PERIOD - 8; i++) {
            advance();
            tickPlayer();
        }
        if (AttackTimer.click(p(), mob, AttackClock.Kind.MELEE) || !AttackTimer.hasPending(p())) {
            throw failure("a click 8 MC early was not buffered");
        }
        while (ActionManager.mcTick() < t0 + WHIP_PERIOD) {
            if (HITS.size() != 1) {
                throw failure("the buffered click resolved early, at +" + (HITS.get(HITS.size() - 1) - t0));
            }
            advance();
            tickPlayer();
        }
        if (!since(t0).equals(List.of(0L, (long) WHIP_PERIOD))) {
            throw failure("resolved at " + since(t0) + ", expected [0, " + WHIP_PERIOD + "]");
        }
        return "buffered at +40, resolved at +" + WHIP_PERIOD;
    }

    /** (c) Switching hotbar slots drops a buffered click. */
    private String slotChange() {
        LivingEntity mob = dummy();
        ItemStack whip = hold("abyssal_whip", 1);
        int slot = p().getInventory().selected;
        int other = (slot + 1) % 9;
        p().getInventory().setItem(other, whip.copy()); // the same weapon, so only the slot changes
        ready();
        watch();
        AttackTimer.click(p(), mob, AttackClock.Kind.MELEE);
        for (int i = 0; i < 10; i++) {
            advance();
            tickPlayer();
        }
        AttackTimer.click(p(), mob, AttackClock.Kind.MELEE);
        if (!AttackTimer.hasPending(p())) {
            throw failure("the early click was not buffered");
        }
        try {
            p().getInventory().selected = other;
            tickPlayer();
            if (AttackTimer.hasPending(p())) {
                throw failure("a hotbar slot change kept the buffered click");
            }
            for (int i = 0; i < 2 * WHIP_PERIOD; i++) {
                advance();
                tickPlayer();
            }
        } finally {
            p().getInventory().selected = slot;
        }
        if (HITS.size() != 1) {
            throw failure(HITS.size() + " attacks resolved; the dropped click must not fire");
        }
        return "dropped on the slot change";
    }

    /** (d) The left-click entry point: SUCCESS and one resolve for a Gielinor monster, PASS for a player or outside Gielinor. */
    private String leftClick() {
        LivingEntity mob = dummy();
        hold("abyssal_whip", 1);
        ready();
        watch();
        InteractionResult r = AttackEntityCallback.EVENT.invoker().interact(p(), c.world, InteractionHand.MAIN_HAND, mob, null);
        if (r != InteractionResult.SUCCESS) {
            throw failure("left-clicking a Gielinor " + DUMMY + " returned " + r + ", expected SUCCESS (vanilla attack cancelled)");
        }
        if (HITS.size() != 1) {
            throw failure("left-clicking a ready player resolved " + HITS.size() + " attacks, expected 1");
        }
        ServerPlayer other = c.realPlayer() ? com.runecraft.platform.FakePlayer.get(c.world) : p();
        InteractionResult rp = AttackEntityCallback.EVENT.invoker().interact(p(), c.world, InteractionHand.MAIN_HAND, other, null);
        if (rp != InteractionResult.PASS) {
            throw failure("left-clicking a player returned " + rp + ", expected PASS");
        }
        Entity outside = EntityType.ZOMBIE.create(c.server.overworld());
        InteractionResult ro = AttackEntityCallback.EVENT.invoker().interact(p(), c.world, InteractionHand.MAIN_HAND, outside, null);
        if (ro != InteractionResult.PASS) {
            throw failure("left-clicking a mob outside Gielinor returned " + ro + ", expected PASS");
        }
        if (HITS.size() != 1) {
            throw failure("the PASS cases resolved attacks (" + HITS.size() + ")");
        }
        return "monster SUCCESS + 1 attack, player PASS, overworld PASS";
    }

    /** (e) A staff with an autocast spell left-clicked on a monster casts: Magic XP, no Attack/Strength/Defence XP. */
    private String staffAutocast() {
        LivingEntity mob = dummy();
        hold("staff_of_fire", 1);
        Spell spell = Spell.FIRE_STRIKE;
        for (Map.Entry<Rune, Integer> e : spell.runes().entrySet()) {
            Inv.give(p(), e.getKey().itemId(), e.getValue() * 2);
        }
        MagicService.setAutocast(p(), spell);
        if (data().autocast() != spell) {
            throw failure("could not autocast " + spell.displayName());
        }
        ready();
        watch();
        int magic = data().skills().xpTenths(Skill.MAGIC);
        int melee = data().skills().xpTenths(Skill.ATTACK) + data().skills().xpTenths(Skill.STRENGTH)
                + data().skills().xpTenths(Skill.DEFENCE);
        InteractionResult r = AttackEntityCallback.EVENT.invoker().interact(p(), c.world, InteractionHand.MAIN_HAND, mob, null);
        int gained = data().skills().xpTenths(Skill.MAGIC) - magic;
        int meleeGained = data().skills().xpTenths(Skill.ATTACK) + data().skills().xpTenths(Skill.STRENGTH)
                + data().skills().xpTenths(Skill.DEFENCE) - melee;
        if (r != InteractionResult.SUCCESS || gained < spell.xpTenths()) {
            throw failure("left-click with an autocasting staff: " + r + ", +" + gained / 10.0 + " Magic XP (expected at least "
                    + spell.xpTenths() / 10.0 + ")");
        }
        if (meleeGained != 0) {
            throw failure("left-click with an autocasting staff gave " + meleeGained / 10.0 + " melee XP: it hit in melee");
        }
        if (AttackTimer.remaining(p()) != AttackClock.periodMc(5)) {
            throw failure("the cast stamped " + AttackTimer.remaining(p()) + " MC, expected a spell's 60");
        }
        return "cast " + spell.displayName() + ", +" + gained / 10.0 + " Magic XP, no melee XP";
    }

    /** (f) A magic shortbow held to fire for 480 MC: 10 arrows on Accurate, 14 on Rapid; tap spam at most 10; never worn. */
    private String bow() {
        ItemStack bow = hold("magic_shortbow", 1);
        if (!(bow.getItem() instanceof OsrsEquipmentItems.Bow item)) {
            throw failure("magic_shortbow is not an OSRS bow item");
        }
        Inv.give(p(), "rune_arrow", 64 * 6);
        takeArrows();
        int[] held = new int[2];
        CombatStyle[] styles = {CombatStyle.RANGED_ACCURATE, CombatStyle.RAPID};
        int duration = bow.getUseDuration(p());
        for (int s = 0; s < styles.length; s++) {
            data().setStyle(styles[s]);
            ready();
            p().startUsingItem(InteractionHand.MAIN_HAND);
            for (int i = 0; i < WINDOW_MC; i++) {
                tickPlayer();
                item.onUseTick(c.world, p(), bow, duration - i);
                advance();
            }
            p().stopUsingItem();
            held[s] = takeArrows();
        }
        if (held[0] != 10 || held[1] != 14) {
            throw failure("holding the bow for " + WINDOW_MC + " MC fired " + held[0] + " arrows on Accurate (expected 10) and "
                    + held[1] + " on Rapid (expected 14)");
        }
        // Tap spam: a 3 MC draw released every 4 MC.
        data().setStyle(CombatStyle.RANGED_ACCURATE);
        ready();
        for (int i = 0; i < WINDOW_MC; i++) {
            tickPlayer();
            if (i % 4 == 3) {
                item.releaseUsing(bow, c.world, p(), duration - AttackClock.TAP_DRAW_MC);
            }
            advance();
        }
        int tapped = takeArrows();
        if (tapped < 1 || tapped > 10) {
            throw failure("tap-releasing every 4 MC for " + WINDOW_MC + " MC fired " + tapped + " arrows (expected 1..10)");
        }
        int shots = 0;
        for (int i = 0; i < 500; i++) {
            shots += item.fire(p(), InteractionHand.MAIN_HAND, bow, null) ? 1 : 0;
            if (i % 50 == 49) {
                takeArrows();
                Inv.give(p(), "rune_arrow", 50);
            }
        }
        takeArrows();
        if (shots != 500 || bow.isDamageableItem() || bow.getDamageValue() != 0 || p().getMainHandItem() != bow) {
            throw failure(shots + " shots; the bow wears out (damageable " + bow.isDamageableItem() + ", damage "
                    + bow.getDamageValue() + ")");
        }
        return "held: 10 Accurate / 14 Rapid in 24 s, taps: " + tapped + ", then 500 shots and no wear";
    }

    /** (g) Melee straight after a bow shot waits the shot's period; a thrown weapon shares the same clock. */
    private String shared() {
        LivingEntity mob = dummy();
        ItemStack bow = hold("magic_shortbow", 1);
        Inv.give(p(), "rune_arrow", 10);
        data().setStyle(CombatStyle.RANGED_ACCURATE);
        ready();
        watch();
        long t0 = ActionManager.mcTick();
        if (!(bow.getItem() instanceof OsrsEquipmentItems.Bow item) || !item.fire(p(), InteractionHand.MAIN_HAND, bow, mob)) {
            throw failure("the magic shortbow did not fire");
        }
        takeArrows(); // never lands: only the melee below counts
        hold("abyssal_whip", 1);
        if (AttackTimer.click(p(), mob, AttackClock.Kind.MELEE)) {
            throw failure("melee resolved right after a bow shot");
        }
        for (int i = 0; i < 2 * WHIP_PERIOD && HITS.isEmpty(); i++) {
            advance();
            tickPlayer();
        }
        long shot = AttackClock.periodMc(4);
        if (!since(t0).equals(List.of(shot))) {
            throw failure("melee after a bow shot resolved at " + since(t0) + ", expected [" + shot + "]");
        }
        // A thrown dart right after that melee does nothing; once the whip's period has passed it flies.
        ItemStack darts = hold("rune_dart", 5);
        p().getCooldowns().removeCooldown(darts.getItem()); // a fake player's item cooldowns only run in these checks
        UseItemCallback.EVENT.invoker().interact(p(), c.world, InteractionHand.MAIN_HAND);
        int early = takeArrows();
        long melee = ActionManager.mcTick();
        while (ActionManager.mcTick() < melee + WHIP_PERIOD) {
            advance();
            tickPlayer();
        }
        UseItemCallback.EVENT.invoker().interact(p(), c.world, InteractionHand.MAIN_HAND);
        int thrown = takeArrows();
        if (early != 0 || thrown != 1) {
            throw failure("a dart thrown on the melee tick flew " + early + " (expected 0), after the whip's period " + thrown
                    + " (expected 1)");
        }
        long dartPeriod = AttackTimer.remaining(p());
        if (dartPeriod != AttackClock.periodMc(3)) {
            throw failure("the dart stamped " + dartPeriod + " MC, expected 36 (3 ticks)");
        }
        return "melee waited the shot's " + shot + " MC; the dart waited the whip, then stamped " + dartPeriod + " MC ("
                + darts.getCount() + " darts left)";
    }

    /** (h) Auto-retaliate on the clock: 10 attacks in 480 MC alone, and still 10 with a manual click on the same target every tick. */
    private String retaliate() {
        LivingEntity mob = dummy();
        hold("abyssal_whip", 1);
        AttackTimer.forget(p().getUUID());
        data().lastAttackTick = -1_000_000;
        data().setAutoRetaliate(true);
        CombatHandler.clearCooldown(mob);
        CombatHandler.npcAttack(mob, p(), AttackType.CRUSH);
        heal(mob);
        if (!AutoRetaliate.retaliating(p(), mob)) {
            throw new SelfTest.Skip("the " + DUMMY + "'s hit did not start auto-retaliate (single-way combat refused it?)");
        }
        watch();
        long t0 = ActionManager.mcTick();
        for (int i = 0; i < WINDOW_MC; i++) {
            tickPlayer();
            heal(mob);
            advance();
        }
        List<Long> alone = since(t0);
        HITS.clear();
        long t1 = ActionManager.mcTick();
        for (int i = 0; i < WINDOW_MC; i++) {
            tickPlayer();
            AttackTimer.click(p(), mob, AttackClock.Kind.MELEE);
            heal(mob);
            advance();
        }
        List<Long> merged = since(t1);
        List<Long> want = every(WHIP_PERIOD, WINDOW_MC / WHIP_PERIOD);
        if (!alone.equals(want)) {
            throw failure("auto-retaliate alone attacked at " + alone + ", expected " + want);
        }
        if (!merged.equals(want)) {
            throw failure("auto-retaliate plus a click every tick attacked at " + merged + ", expected " + want
                    + " (never a second cooldown)");
        }
        return "auto-retaliate 10 attacks, with clicks still 10";
    }

    /** (i) The Nightmare's spores (lastAttackTick += slow x 12 on the attack tick) still delay the next attack by slow ticks. */
    private String nightmare() {
        LivingEntity mob = dummy();
        hold("abyssal_whip", 1);
        ready();
        watch();
        nightmareSlow = 2;
        long t0 = ActionManager.mcTick();
        for (int i = 0; i < 3 * (WHIP_PERIOD + 24); i++) {
            tickPlayer();
            AttackTimer.click(p(), mob, AttackClock.Kind.MELEE);
            heal(mob);
            advance();
        }
        long gap = WHIP_PERIOD + 2L * ActionManager.MC_TICKS_PER_GAME_TICK;
        List<Long> want = List.of(0L, gap, 2 * gap);
        if (!since(t0).equals(want)) {
            throw failure("slowed attacks resolved at " + since(t0) + ", expected " + want);
        }
        return "slowed by 2 ticks: attacks every " + gap + " MC";
    }

    /** (j) Vanilla's indicator takes the OSRS period: dragon axe 60 MC, whip 48, staff of fire 60, unarmed (Gielinor) 48. */
    private String indicator() {
        double axe = AttackTimer.vanillaPeriodMc(Inv.stack("dragon_axe", 1));
        double whip = AttackTimer.vanillaPeriodMc(Inv.stack("abyssal_whip", 1));
        double staff = AttackTimer.vanillaPeriodMc(Inv.stack("staff_of_fire", 1));
        if (Math.abs(axe - 60) > 0.01 || Math.abs(whip - 48) > 0.01 || Math.abs(staff - 60) > 0.01) {
            throw failure(String.format(java.util.Locale.ROOT, "vanilla periods: dragon axe %.2f (60), whip %.2f (48), staff of fire %.2f (60)",
                    axe, whip, staff));
        }
        p().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        AttackTimer.tickForSelfTest(p());
        AttributeInstance speed = p().getAttribute(Attributes.ATTACK_SPEED);
        double unarmed = AttackClock.vanillaPeriodMc(speed == null ? 4.0 : speed.getValue());
        if (Math.abs(unarmed - 48) > 0.01) {
            throw failure(String.format(java.util.Locale.ROOT, "unarmed in Gielinor the vanilla period is %.2f MC, expected 48", unarmed));
        }
        boolean[] reach = {false};
        Inv.stack("dragon_halberd", 1).forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.value() == Attributes.ENTITY_INTERACTION_RANGE.value() && modifier.amount() >= 1.0) {
                reach[0] = true;
            }
        });
        if (!reach[0]) {
            throw failure("a dragon halberd has no extra reach (OSRS: 2 tiles)");
        }
        return String.format(java.util.Locale.ROOT, "axe %.0f, whip %.0f, staff %.0f, unarmed %.0f MC; halberd +1 reach",
                axe, whip, staff, unarmed);
    }

    /**
     * {@code boss:playability_attack_buffer}, also for the real player: a click 8 MC early resolves on the ready tick through
     * the per-MC-tick path ({@link PlayerEvents#everyMcTick} for a real player). Counted with the afterHit listener, not health.
     */
    private String realBuffer() {
        LivingEntity mob = dummy();
        hold("abyssal_whip", 1);
        ready();
        watch();
        long t0 = ActionManager.mcTick();
        AttackTimer.click(p(), mob, AttackClock.Kind.MELEE);
        for (int i = 0; i < WHIP_PERIOD - 8; i++) {
            advance();
            tickPlayer();
            heal(mob);
        }
        boolean buffered = !AttackTimer.click(p(), mob, AttackClock.Kind.MELEE);
        while (ActionManager.mcTick() < t0 + WHIP_PERIOD) {
            advance();
            tickPlayer();
            heal(mob);
        }
        if (!buffered || !since(t0).equals(List.of(0L, (long) WHIP_PERIOD))) {
            throw failure("the early click " + (buffered ? "was buffered" : "was not buffered") + " and attacks resolved at "
                    + since(t0) + ", expected [0, " + WHIP_PERIOD + "]");
        }
        return (c.realPlayer() ? "real player via everyMcTick" : "fake player via tickForSelfTest") + ": buffered 8 MC early, resolved at +"
                + WHIP_PERIOD;
    }
}
