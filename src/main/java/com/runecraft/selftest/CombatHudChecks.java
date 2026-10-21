package com.runecraft.selftest;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.DataBossService;
import com.runecraft.boss.Mechanics;
import com.runecraft.content.bosses.ScriptedBoss;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.PoisonService;
import com.runecraft.combat.PrayerService;
import com.runecraft.content.systems.SpecialAttackService;
import com.runecraft.content.systems.SysState;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.item.OsrsEquipmentItems;
import com.runecraft.network.HudNet;
import com.runecraft.network.HudProbe;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.AmmoRecovery;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.CastBar;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.osrs.data.Monster;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.registry.ModEntities;
import com.runecraft.skill.QuickConsume;
import com.runecraft.util.Inv;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Round 42 combat HUD checks ({@code hud:*}, run by the client walkthrough's real-player self-test too): the telegraph cast
 * bar, the opponent info box, the curse map, the poison tint, the auto-retaliate hint, quick-eat / quick-drink, the special
 * attack cleared on a weapon switch, the boss bar status and ammo landing under the target. Every check asserts what the
 * server sends (packets through their recorders, state); with the real player on the integrated server it also waits for the
 * client's HUD ({@link HudProbe}) to show it: the right style, key, frame, name and hitpoints on screen.
 */
final class CombatHudChecks {
    /** How long a check waits for the local client's HUD to show what the server sent. */
    private static final long CLIENT_WAIT_MS = 3000;

    private final TestContext c;

    private CombatHudChecks(TestContext c) {
        this.c = c;
    }

    static List<SelfTest.Check> all(TestContext context) {
        CombatHudChecks g = new CombatHudChecks(context);
        List<SelfTest.Check> checks = new ArrayList<>();
        checks.add(new SelfTest.Check("hud:cast_bar", g::castBar));
        checks.add(new SelfTest.Check("hud:info_box", g::infoBox));
        checks.add(new SelfTest.Check("hud:curse_map", g::curseMap));
        checks.add(new SelfTest.Check("hud:poison_and_retaliate", g::poisonAndRetaliate));
        checks.add(new SelfTest.Check("hud:quick_eat", g::quickEat));
        checks.add(new SelfTest.Check("hud:special_switch", g::specialSwitch));
        checks.add(new SelfTest.Check("hud:boss_bar_status", g::bossBarStatus));
        checks.add(new SelfTest.Check("hud:ammo_landing", g::ammoLanding));
        checks.add(new SelfTest.Check("hud:ground_casts", g::groundCasts));
        checks.add(new SelfTest.Check("hud:builtin_boss_bar", g::builtinBossBar));
        checks.add(new SelfTest.Check("hud:switch_countdown", g::switchCountdown));
        checks.add(new SelfTest.Check("hud:grapple_prompt", g::grapplePrompt));
        checks.add(new SelfTest.Check("hud:food_delay", g::foodDelay));
        return checks;
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

    private static void expect(boolean ok, String message) {
        if (!ok) {
            throw failure(message);
        }
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
            mob.setNoAi(true);
        }
        c.world.addFreshEntity(living);
        return living;
    }

    /** Whether the local client's HUD can be checked: the real player with the client in this JVM drawing a probe. */
    private boolean client() {
        return c.realPlayer() && HudProbe.latest != null;
    }

    /**
     * Waits (on the server thread, a few seconds at most) until the client's HUD shows what {@code ok} wants; the client
     * runs on its own thread and reads the packets sent so far, flushed first. Returns the snapshot, or null on time-out.
     */
    private HudProbe.Snapshot awaitClient(Predicate<HudProbe.Snapshot> ok) {
        if (p().connection != null) {
            p().connection.getConnection().flushChannel();
        }
        long end = System.currentTimeMillis() + CLIENT_WAIT_MS;
        while (System.currentTimeMillis() < end) {
            HudProbe.Snapshot s = HudProbe.latest;
            if (s != null && ok.test(s)) {
                return s;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    /** The cast-bar row from {@code source} whose key says the prayer only lowers the hit, or null. */
    private static HudProbe.Cast reducedFrom(HudProbe.Snapshot s, int source) {
        for (HudProbe.Cast cast : s.casts()) {
            if (cast.source() == source && cast.key().endsWith(CastBar.REDUCED_SUFFIX)) {
                return cast;
            }
        }
        return null;
    }

    private static HudProbe.Cast castFrom(HudProbe.Snapshot s, int source) {
        for (HudProbe.Cast cast : s.casts()) {
            if (cast.source() == source) {
                return cast;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ cast bar

    /**
     * A telegraphed magic attack puts the attacker on the target's cast bar with the magic style, its lead and (on the client)
     * the Protect from Magic key in a red frame, turning green once Protect from Magic is up.
     */
    private String castBar() {
        LivingEntity spawned = spawn(Monster.BLACK_DEMON.entity(), 3, 0);
        if (!(spawned instanceof OsrsMonsterEntity mob)) {
            throw new SelfTest.Skip("the black demon isn't an OSRS monster entity here");
        }
        List<HudNet.SentCast> sent = new ArrayList<>();
        java.util.function.Consumer<HudNet.SentCast> before = HudNet.castListener;
        HudNet.castListener = s -> {
            before.accept(s);
            if (s.player() == p()) {
                sent.add(s);
            }
        };
        String note;
        try {
            data().activePrayers().clear();
            ServerNetworking.sync(p()); // the client's prayer HUD starts with no overhead (ordered before the cast)
            AttackDef blast = AttackDef.of("hud_test_blast", AttackType.MAGIC, 20, 20, 1).telegraph(2, "The demon gathers dark magic!");
            Mechanics.perform(mob, p(), blast, 1.0);
            expect(sent.size() == 1, "a telegraphed attack sent " + sent.size() + " cast-bar packets to its target");
            HudNet.SentCast cast = sent.get(0);
            expect(cast.source() == mob.getId() && cast.style() == CastBar.MAGIC && cast.flags() == 0,
                    "the cast names " + cast.source() + " style " + cast.style() + " flags " + cast.flags() + ", expected the demon "
                            + mob.getId() + ", magic");
            expect(cast.landInMc() >= com.runecraft.osrs.ThreatTiming.TELEGRAPH_FLOOR_MC,
                    "the cast lands in " + cast.landInMc() + " MC, under the 36 MC telegraph floor");
            note = "magic, lands in " + cast.landInMc() + " MC";
            if (client()) {
                HudProbe.Snapshot shown = awaitClient(s -> castFrom(s, mob.getId()) != null);
                expect(shown != null, "the client's cast bar never showed the demon's telegraph");
                HudProbe.Cast row = castFrom(shown, mob.getId());
                expect(row.style() == CastBar.MAGIC && row.protection() == 0, "the cast bar row has style " + row.style()
                        + " and protection " + row.protection() + ", expected magic / Protect from Magic");
                expect(row.key().equals(shown.protectionKeys()[0]) && !row.key().equals("?"),
                        "the row shows key '" + row.key() + "', the Protect from Magic key is '" + shown.protectionKeys()[0] + "'");
                expect(Boolean.FALSE.equals(row.rightOverhead()), "with no overhead the frame isn't red: " + row.rightOverhead());
                expect(row.name().equals(mob.getName().getString()), "the row names '" + row.name() + "'");
                PrayerService.set(p(), Prayer.PROTECT_FROM_MAGIC, true);
                ServerNetworking.sync(p());
                HudProbe.Snapshot green = awaitClient(s -> castFrom(s, mob.getId()) != null
                        && Boolean.TRUE.equals(castFrom(s, mob.getId()).rightOverhead()));
                expect(green != null, "the frame didn't turn green with Protect from Magic up");
                note += "; client: '" + row.name() + "' MAG [" + row.key() + "] " + row.secondsLeft() + "s, red then green";
            } else {
                note += " (no client: server packet only)";
            }
            // Dragonfire (Elvarg's is telegraphed): Protect from Magic lowers it (Mechanics.hit; OSRS Wiki, Dragonfire), so the row
            // names the Protect from Magic key with "part" and the right/wrong frame, never "no prayer".
            sent.clear();
            AttackDef fire = AttackDef.of("hud_test_dragonfire", AttackType.MAGIC, 20, 20, 1)
                    .special(AttackDef.Special.DRAGONFIRE, 0).telegraph(2, "The demon breathes fire!");
            Mechanics.perform(mob, p(), fire, 1.0);
            expect(sent.size() == 1 && sent.get(0).style() == CastBar.MAGIC && (sent.get(0).flags() & CastBar.FLAG_UNPRAYABLE) == 0
                    && (sent.get(0).flags() & CastBar.FLAG_REDUCED) != 0, "a telegraphed dragonfire sent " + sent
                    + ", expected magic, lowered by Protect from Magic");
            note += "; dragonfire: Protect from Magic lowers it";
            if (client()) {
                HudProbe.Snapshot fireShown = awaitClient(s -> reducedFrom(s, mob.getId()) != null);
                expect(fireShown != null, "the client's cast bar never showed the dragonfire with a 'part' key");
                HudProbe.Cast row = reducedFrom(fireShown, mob.getId());
                expect(row.protection() == 0 && row.key().equals(fireShown.protectionKeys()[0] + CastBar.REDUCED_SUFFIX),
                        "the dragonfire row shows key '" + row.key() + "' for protection " + row.protection()
                                + ", expected the Protect from Magic key '" + fireShown.protectionKeys()[0] + "' with 'part'");
                expect(Boolean.TRUE.equals(row.rightOverhead()), "Protect from Magic is up and the dragonfire frame is "
                        + row.rightOverhead() + ", not green");
                note += " [" + row.key() + "] green";
            }
            // Jad's telegraph goes through the same bar (ThreatCues.announce with its 3-tick lead).
            sent.clear();
            com.runecraft.boss.ThreatCues.announce(mob, p(), "The demon stomps!", AttackType.RANGED, 36);
            expect(sent.size() == 1 && sent.get(0).style() == CastBar.RANGED && sent.get(0).landInMc() == 36,
                    "an announced telegraph sent " + sent);
        } finally {
            HudNet.castListener = before;
            data().activePrayers().clear();
            mob.discard();
            Mechanics.tickForSelfTest(); // the discarded demon's telegraph is dropped
        }
        return note;
    }

    // ------------------------------------------------------------------ opponent info box

    /** A hit on a monster sends its exact hitpoints; the client's info box shows its name, level and those hitpoints. */
    private String infoBox() {
        com.runecraft.combat.CombatZones.forget(p());
        LivingEntity mob = spawn(Monster.BLACK_DEMON.entity(), 3, 0);
        try {
            ServerNetworking.LAST_SPLAT = null;
            int dealt = CombatHandler.applyDamage(p(), mob, 7);
            ServerNetworking.Splat splat = ServerNetworking.LAST_SPLAT;
            expect(splat != null && splat.target() == mob.getId() && splat.attacker() == p().getId(),
                    "the hit sent " + splat + ", expected a splat on the demon from the player");
            int[] hp = HudNet.osrsHitpoints(mob);
            expect(dealt == 7, "the 7 damage hit dealt " + dealt);
            expect(splat.hp() == hp[0] && splat.maxHp() == hp[1] && hp[0] < hp[1],
                    "the splat carries " + splat.hp() + "/" + splat.maxHp() + ", the demon has " + hp[0] + "/" + hp[1]);
            String note = "splat " + splat.hp() + "/" + splat.maxHp();
            if (client()) {
                HudProbe.Snapshot s = awaitClient(x -> x.opponent() == mob.getId() && x.opponentHp() == hp[0]);
                expect(s != null, "the client's info box never showed the demon at " + hp[0] + " hitpoints (last: "
                        + (HudProbe.latest == null ? "none" : HudProbe.latest.opponent() + " " + HudProbe.latest.opponentHp()) + ")");
                expect(s.opponentMaxHp() == hp[1] && s.opponentName().equals(mob.getName().getString()) && s.opponentLevel() > 0,
                        "the info box shows " + s.opponentName() + " level " + s.opponentLevel() + " " + s.opponentHp() + "/"
                                + s.opponentMaxHp());
                note += "; client box: " + s.opponentName() + " (level-" + s.opponentLevel() + ") " + s.opponentHp() + "/"
                        + s.opponentMaxHp();
            }
            return note;
        } finally {
            mob.discard();
            com.runecraft.combat.CombatZones.forget(p());
        }
    }

    // ------------------------------------------------------------------ curse map

    /** The Nightmare's curse reaches the client as a key map; the cast bar then names the key that really blocks. */
    private String curseMap() {
        ServerPlayer p = p();
        BiFunction<ServerPlayer, Prayer, Prayer> curse = (pl, pr) -> pl == p
                ? com.runecraft.osrs.content.bosses.NightmareRules.cursed(pr) : pr;
        HudNet.update(p);
        PrayerService.REDIRECTS.add(curse);
        String note;
        try {
            HudNet.update(p);
            int[] sent = HudNet.lastSent(p);
            expect(sent != null && sent[0] == 1 && sent[1] == 2 && sent[2] == 0,
                    "the cursed key map sent is " + (sent == null ? "nothing" : Arrays.toString(Arrays.copyOf(sent, 3))));
            note = "sent " + Arrays.toString(Arrays.copyOf(sent, 3));
            if (client()) {
                HudProbe.Snapshot s = awaitClient(x -> Arrays.equals(x.curse(), new int[]{1, 2, 0}));
                expect(s != null, "the client never got the curse map");
                HudNet.cast(p, null, CastBar.MAGIC, 0, 40, "Curse check");
                HudProbe.Snapshot k = awaitClient(x -> x.casts().stream().anyMatch(cst -> cst.name().equals("Curse check")));
                expect(k != null, "the client never showed the curse check cast");
                HudProbe.Cast row = k.casts().stream().filter(cst -> cst.name().equals("Curse check")).findFirst().orElseThrow();
                expect(row.key().equals(k.protectionKeys()[2]), "cursed, a magic cast shows key '" + row.key()
                        + "'; Protect from Magic is now switched by the melee key '" + k.protectionKeys()[2] + "'");
                note += "; client map " + Arrays.toString(s.curse()) + ", magic blocked with [" + row.key() + "]";
            }
        } finally {
            PrayerService.REDIRECTS.remove(curse);
        }
        HudNet.update(p);
        int[] after = HudNet.lastSent(p);
        expect(after[0] == 0 && after[1] == 1 && after[2] == 2, "the curse map stayed " + Arrays.toString(Arrays.copyOf(after, 3)));
        if (client()) {
            expect(awaitClient(x -> Arrays.equals(x.curse(), new int[]{0, 1, 2})) != null, "the client kept the curse map");
        }
        return note;
    }

    // ------------------------------------------------------------------ poison tint and auto-retaliate

    /** Poison and venom tint the HP orb; a monster's hit with auto-retaliate on names it in the hint. */
    private String poisonAndRetaliate() {
        ServerPlayer p = p();
        long tick = ActionManager.gameTick();
        PoisonService.clear(p);
        HudNet.update(p);
        PoisonService.poison(p, 6, tick);
        HudNet.update(p);
        expect(HudNet.lastSent(p)[3] == 1, "a poisoned player sent poison state " + HudNet.lastSent(p)[3]);
        if (client()) {
            expect(awaitClient(x -> x.poison() == 1) != null, "the client's HP orb never turned poisoned");
        }
        PoisonService.venom(p, tick);
        HudNet.update(p);
        expect(HudNet.lastSent(p)[3] == 2, "an envenomed player sent poison state " + HudNet.lastSent(p)[3]);
        if (client()) {
            expect(awaitClient(x -> x.poison() == 2) != null, "the client's HP orb never turned envenomed");
        }
        PoisonService.clear(p);
        HudNet.update(p);
        expect(HudNet.lastSent(p)[3] == 0, "a cured player still sends poison state " + HudNet.lastSent(p)[3]);

        LivingEntity mob = spawn(Monster.BLACK_DEMON.entity(), 2, 0);
        try {
            expect(data().autoRetaliate(), "auto-retaliate is off on fresh player data");
            data().lastAttackTick = ActionManager.mcTick() - 10_000; // not fighting by hand
            CombatHandler.applyDamage(mob, p, 0);
            c.heal();
            HudNet.update(p);
            int target = HudNet.lastSent(p)[4];
            if (target < 0) {
                throw new SelfTest.Skip("auto-retaliate didn't engage the demon here (combat zones / busy)");
            }
            expect(target == mob.getId(), "auto-retaliate names entity " + target + ", the demon is " + mob.getId());
            String note = "poison 1, venom 2, cured 0; retaliating against " + target;
            if (client()) {
                String want = "Auto-retaliating: " + mob.getName().getString();
                HudProbe.Snapshot s = awaitClient(x -> x.retaliating().equals(want));
                expect(s != null, "the client never showed '" + want + "'");
                note += "; client: '" + s.retaliating() + "'";
            }
            return note;
        } finally {
            com.runecraft.combat.AutoRetaliate.stop(p);
            mob.discard();
            com.runecraft.combat.CombatZones.forget(p);
            HudNet.update(p);
        }
    }

    // ------------------------------------------------------------------ quick-eat / quick-drink

    /**
     * Quick-eat picks the food that fits the missing hitpoints (the lobster for 15, not the shark), refuses during the eat
     * delay, then eats a karambwan (combo food); quick-drink picks the prayer potion when prayer is low.
     */
    private String quickEat() {
        ServerPlayer p = p();
        p.getInventory().clearContent();
        p.getInventory().selected = 0;
        p.getInventory().setItem(0, Inv.stack("shark", 1));
        p.getInventory().setItem(5, Inv.stack("lobster", 2));
        p.getInventory().setItem(7, Inv.stack("cooked_karambwan", 1));
        p.getInventory().setItem(9, Inv.stack("prayer_potion", 1));
        p.getInventory().setItem(10, Inv.stack("super_attack", 1));
        for (int slot : new int[]{0, 5, 7, 9, 10}) {
            ItemStack s = p.getInventory().getItem(slot);
            expect(!s.isEmpty(), "item in slot " + slot + " is not registered");
            p.getCooldowns().removeCooldown(s.getItem());
        }
        float max = p.getMaxHealth();
        p.setHealth(max - 15);
        boolean ate = QuickConsume.use(p, false);
        expect(ate && "lobster".equals(QuickConsume.lastUsed), "quick-eat at 15 missing ate " + QuickConsume.lastUsed
                + (QuickConsume.lastRefusal == null ? "" : " (" + QuickConsume.lastRefusal + ")"));
        expect(p.getInventory().getItem(5).getCount() == 1 && p.getInventory().getItem(0).getCount() == 1,
                "the lobster stack has " + p.getInventory().getItem(5).getCount() + ", the shark " + p.getInventory().getItem(0).getCount());
        expect(p.getHealth() >= max - 15 + 12 - 0.01f, "health " + p.getHealth() + " after a lobster from " + (max - 15));
        p.setHealth(max - 30);
        boolean second = QuickConsume.use(p, false);
        expect(second && "cooked_karambwan".equals(QuickConsume.lastUsed),
                "during the eat delay quick-eat used " + QuickConsume.lastUsed + " (" + QuickConsume.lastRefusal + "), expected the karambwan combo");
        expect(!QuickConsume.use(p, false), "a third food went down on the same tick: " + QuickConsume.lastUsed);
        data().setPrayerPoints(10);
        boolean drank = QuickConsume.use(p, true);
        expect(drank && "prayer_potion".equals(QuickConsume.lastUsed), "quick-drink at 10 prayer drank " + QuickConsume.lastUsed
                + (QuickConsume.lastRefusal == null ? "" : " (" + QuickConsume.lastRefusal + ")"));
        expect(data().prayerPoints() > 10, "prayer points " + data().prayerPoints() + " after a prayer potion from 10");
        expect(p.getInventory().getItem(9).getDamageValue() == 1, "the prayer potion has damage " + p.getInventory().getItem(9).getDamageValue());
        expect(p.getOffhandItem().isEmpty(), "the off hand kept " + p.getOffhandItem());
        c.heal();
        return "lobster at 15 missing, karambwan combo, third refused, prayer potion at 10 prayer";
    }

    // ------------------------------------------------------------------ special attack on a weapon switch

    /** A cost the self-test's cost rule gives the special attacks of {@link #testCostPlayer} (-1: none). */
    private static volatile int testCost = -1;
    private static volatile java.util.UUID testCostPlayer;
    private static boolean costRuleAdded;

    /**
     * A special armed on the dragon dagger is deselected by the switch itself (on the server, not on the next game tick): a
     * flick to food and back leaves it off, as does the whip, and logging out. On the client, "Special ready" shows for the
     * dagger and not for a weapon without a special, judged by the cost the server sends (a cost rule over the energy: no cue).
     */
    private String specialSwitch() {
        ServerPlayer p = p();
        p.getInventory().clearContent();
        int sel = p.getInventory().selected;
        ItemStack dagger = Inv.stack("dragon_dagger", 1);
        p.getInventory().setItem(sel, dagger);
        expect(!p.getMainHandItem().isEmpty(), "no dragon dagger item");
        SysState.SysPlayer sp = SysState.of(p);
        sp.specEnergy = 100;
        sp.specArmed = false;
        SpecialAttackService.toggle(p);
        expect(sp.specArmed, "the dragon dagger's special didn't arm");
        expect(!SpecialAttackService.disarmedBySwitch(p) && sp.specArmed, "the special disarmed with the dagger still in hand");
        // a flick to a shark and back inside one game tick: the switch itself deselects it, so the next stab is no special
        ItemStack shark = Inv.stack("shark", 1);
        p.getInventory().setItem(sel, shark);
        SpecialAttackService.heldChanged(p, dagger, shark);
        expect(!sp.specArmed, "switching to a shark left the special armed until the next game tick");
        ItemStack back = Inv.stack("dragon_dagger", 1);
        p.getInventory().setItem(sel, back);
        SpecialAttackService.heldChanged(p, shark, back);
        expect(!sp.specArmed && !SpecialAttackService.isSpecial(p, AttackType.STAB),
                "flicking back to the dagger kept the special armed");
        // the same weapon's data changing (charges) is no switch
        SpecialAttackService.toggle(p);
        expect(sp.specArmed, "the dragon dagger's special didn't arm again");
        SpecialAttackService.heldChanged(p, back, back.copy());
        expect(sp.specArmed, "a change of the held dagger's data deselected the special");
        ItemStack whip = Inv.stack("abyssal_whip", 1);
        p.getInventory().setItem(sel, whip);
        SpecialAttackService.heldChanged(p, back, whip);
        expect(!sp.specArmed, "switching to the whip left the special armed");
        expect(!SpecialAttackService.isSpecial(p, AttackType.SLASH), "the whip's attack still counts as the armed special");
        // logging out deselects it too
        p.getInventory().setItem(sel, back);
        SpecialAttackService.heldChanged(p, whip, back);
        SpecialAttackService.toggle(p);
        expect(sp.specArmed, "the dragon dagger's special didn't arm a third time");
        SpecialAttackService.loggedOut(p);
        expect(!sp.specArmed, "logging out left the special armed");
        String note = "deselected by the switch to a shark (and back), the whip and logging out; kept on a data change";
        if (client()) {
            if (!costRuleAdded) {
                costRuleAdded = true;
                SpecialAttackService.addCostRule((pl, spec) -> pl.getUUID().equals(testCostPlayer) ? testCost : -1);
            }
            // The server thread waits below, so the equipment event can't fire: each switch calls the server's handler (what
            // the event does on the next MC tick) after the slot update, which sends the state the client's cue reads.
            ItemStack scimitar = Inv.stack("rune_scimitar", 1);
            switchTo(p, sel, back, scimitar);
            HudProbe.Snapshot plain = awaitClient(x -> x.held().equals("rune_scimitar"));
            expect(plain != null, "the client never held the rune scimitar (held " + (HudProbe.latest == null ? "?" : HudProbe.latest.held()) + ")");
            expect(!plain.specialReady(), "'Special ready' showed for a rune scimitar, which has no special attack");
            // the cost the server sends decides: a cost rule at 60 over 50 energy shows no cue, though the base cost is 25
            try {
                testCostPlayer = p.getUUID();
                testCost = 60;
                sp.specEnergy = 50;
                ItemStack costly = Inv.stack("dragon_dagger", 1);
                switchTo(p, sel, scimitar, costly);
                HudProbe.Snapshot held = awaitClient(x -> x.held().equals("dragon_dagger"));
                expect(held != null, "the client never held the dragon dagger");
                long settled = held.clientTick() + SETTLE_TICKS;
                HudProbe.Snapshot later = awaitClient(x -> x.clientTick() >= settled);
                expect(later != null && !later.specialReady(), "'Special ready' showed at 50% energy for a special the server costs at 60%");
                switchTo(p, sel, costly, scimitar);
                expect(awaitClient(x -> x.held().equals("rune_scimitar")) != null, "the client never held the rune scimitar again");
            } finally {
                testCost = -1;
                testCostPlayer = null;
                sp.specEnergy = 100;
            }
            switchTo(p, sel, scimitar, Inv.stack("dragon_dagger", 1));
            HudProbe.Snapshot s = awaitClient(x -> x.held().equals("dragon_dagger") && x.specialReady());
            expect(s != null, "the client never showed 'Special ready' after switching to the dragon dagger");
            note += "; client: no cue for the scimitar or over the server's cost, Special ready for the dagger";
        }
        sp.specArmed = false;
        p.getInventory().clearContent();
        p.inventoryMenu.broadcastChanges();
        return note;
    }

    /** Client ticks a "no cue" check waits for the state that follows a switch. */
    private static final int SETTLE_TICKS = 8;

    /** Puts {@code to} in the selected slot, syncs it, then runs the server's switch handler (it sends the state after the slot). */
    private static void switchTo(ServerPlayer p, int sel, ItemStack from, ItemStack to) {
        p.getInventory().setItem(sel, to);
        p.inventoryMenu.broadcastChanges();
        SpecialAttackService.heldChanged(p, from, to);
    }

    // ------------------------------------------------------------------ boss bar status

    /** The boss bar shows the phase and the style colour: Zulrah's serpentine form green, magma red, tanzanite blue. */
    private String bossBarStatus() {
        StringBuilder note = new StringBuilder();
        for (String[] form : new String[][]{{"zulrah", "GREEN"}, {"zulrah_magma", "RED"}, {"zulrah_tanzanite", "BLUE"}}) {
            BossDef def = ContentRegistry.boss(form[0]);
            expect(def != null, "no boss " + form[0]);
            DataBossEntity boss = DataBossService.spawn(c.world, c.near(4, 0), def, p());
            expect(boss != null, "couldn't spawn " + form[0]);
            try {
                boss.refreshBossBar();
                expect(boss.bossBarColor() == BossEvent.BossBarColor.valueOf(form[1]),
                        form[0] + "'s bar is " + boss.bossBarColor() + ", expected " + form[1]);
                note.append(form[0]).append(' ').append(boss.bossBarColor()).append("; ");
            } finally {
                boss.discard();
            }
        }
        BossDef phased = ContentRegistry.boss("sarachnis"); // two phase thresholds: "Phase 1/3"
        if (phased != null) {
            DataBossEntity boss = DataBossService.spawn(c.world, c.near(4, 0), phased, p());
            if (boss != null) {
                try {
                    boss.refreshBossBar();
                    String title = boss.bossBarTitle();
                    expect(title.startsWith(phased.name) && title.contains("Phase 1/" + (phased.phases.size() + 1)),
                            phased.id + "'s bar reads '" + title + "'");
                    note.append('\'').append(title).append('\'');
                } finally {
                    boss.discard();
                }
            }
        }
        return note.toString();
    }

    // ------------------------------------------------------------------ ammo landing

    /**
     * Fired ammo lands under the target: 200 landings without a device drop about 80% at the target's feet; with Ava's
     * accumulator about 72% come back to the pack and 8% drop; a rune platebody stops it. A real arrow hitting the demon and a
     * shot that misses into the floor land the same way.
     */
    private String ammoLanding() {
        ServerPlayer p = p();
        LivingEntity target = spawn(Monster.BLACK_DEMON.entity(), 5, 0);
        int[] outcomes = new int[3];
        List<Vec3> spots = new ArrayList<>();
        java.util.function.Consumer<OsrsEquipmentItems.AmmoLanding> before = OsrsEquipmentItems.ammoListener;
        OsrsEquipmentItems.ammoListener = l -> {
            before.accept(l);
            if (l.shooter() == p) {
                outcomes[l.outcome().ordinal()]++;
                spots.add(l.at());
            }
        };
        SysState.SysPlayer sp = SysState.of(p);
        try {
            p.getInventory().clearContent();
            sp.worn.remove(Jewellery.Slot.CAPE);
            for (int i = 0; i < 200; i++) {
                OsrsEquipmentItems.landAmmo(p, target.position(), "rune_arrow");
            }
            int dropped = outcomes[AmmoRecovery.Outcome.DROPPED.ordinal()];
            expect(outcomes[AmmoRecovery.Outcome.RECOVERED.ordinal()] == 0, "ammo came back without a device");
            expect(dropped >= 130 && dropped <= 190, dropped + " of 200 arrows dropped without a device (wiki: 80%)");
            int onFloor = arrowsNear(target.position(), 1.5);
            expect(onFloor == dropped, onFloor + " arrows lie under the demon, " + dropped + " dropped");
            String note = "no device: " + dropped + "/200 under the target";

            Arrays.fill(outcomes, 0);
            clearArrows(target.position());
            sp.worn.put(Jewellery.Slot.CAPE, Inv.stack("avas_accumulator", 1));
            expect(OsrsEquipmentItems.ammoDevice(p) == AmmoRecovery.Device.ACCUMULATOR, "Ava's accumulator isn't worn");
            for (int i = 0; i < 200; i++) {
                OsrsEquipmentItems.landAmmo(p, target.position(), "rune_arrow");
            }
            int back = outcomes[AmmoRecovery.Outcome.RECOVERED.ordinal()];
            int down = outcomes[AmmoRecovery.Outcome.DROPPED.ordinal()];
            expect(back >= 115 && back <= 170, back + " of 200 came back with Ava's accumulator (wiki: 72%)");
            expect(down >= 3 && down <= 32, down + " of 200 dropped with Ava's accumulator (wiki: 8%)");
            expect(Inv.count(p, "rune_arrow") == back, Inv.count(p, "rune_arrow") + " arrows in the pack, " + back + " recovered");
            note += "; accumulator: " + back + " back, " + down + " dropped";

            p.setItemSlot(EquipmentSlot.CHEST, Inv.stack("rune_platebody", 1));
            expect(OsrsEquipmentItems.ammoDevice(p) == AmmoRecovery.Device.NONE, "a rune platebody didn't stop Ava's accumulator");
            p.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            sp.worn.remove(Jewellery.Slot.CAPE);

            // A real arrow on the demon (CombatHandler.playerRanged) and one missing into the floor (LandingArrow).
            spots.clear();
            Arrays.fill(outcomes, 0);
            ItemStack bow = Inv.stack("magic_shortbow", 1);
            ItemStack ammo = Inv.stack("rune_arrow", 1);
            if (!(ammo.getItem() instanceof ArrowItem arrowItem)) {
                throw failure("rune_arrow isn't an arrow item");
            }
            AbstractArrow hit = arrowItem.createArrow(c.world, ammo, p, bow);
            hit.setPos(target.getX(), target.getY() + 1, target.getZ() - 0.6);
            if (target instanceof Mob m) {
                m.invulnerableTime = 0;
            }
            target.hurt(p.damageSources().arrow(hit, p), 5f);
            expect(outcomes[0] + outcomes[1] + outcomes[2] == 1, "the arrow on the demon landed " + (outcomes[0] + outcomes[1]
                    + outcomes[2]) + " times");
            expect(spots.get(0).distanceTo(target.position()) < 0.01, "the arrow's ammo landed at " + spots.get(0)
                    + ", the demon stands at " + target.position());
            AbstractArrow miss = arrowItem.createArrow(c.world, ammo, p, bow);
            if (!(miss instanceof OsrsEquipmentItems.LandingArrow)) {
                throw failure("a fired rune arrow isn't a LandingArrow");
            }
            BlockPos floor = c.near(-3, 0);
            miss.setPos(floor.getX() + 0.5, floor.getY() + 1.5, floor.getZ() + 0.5);
            miss.setDeltaMovement(0, -1.5, 0);
            c.world.addFreshEntity(miss);
            for (int i = 0; i < 20 && !miss.isRemoved(); i++) {
                miss.tick();
            }
            expect(miss.isRemoved(), "an arrow fired into the floor is still flying or stuck");
            expect(spots.size() == 2 && spots.get(1).distanceTo(Vec3.atCenterOf(floor)) < 1.5,
                    "the missed arrow's ammo landed at " + (spots.size() > 1 ? spots.get(1) : "nowhere") + ", it struck near " + floor);
            note += "; a real hit landed under the demon, a miss where it struck";
            return note;
        } finally {
            OsrsEquipmentItems.ammoListener = before;
            sp.worn.remove(Jewellery.Slot.CAPE);
            clearArrows(target.position());
            target.discard();
            com.runecraft.combat.CombatZones.forget(p);
        }
    }

    // ------------------------------------------------------------------ round 43: ground strikes, hazards and projectiles

    /** A data boss entity owning probe strikes, created but never added to the world (it never ticks). */
    private DataBossEntity probeHost(Vec3 at) {
        List<String> ids = new ArrayList<>(ModEntities.DATA_BOSS_TYPES.keySet());
        java.util.Collections.sort(ids);
        for (String id : ids) {
            EntityType<DataBossEntity> type = ModEntities.DATA_BOSS_TYPES.get(id);
            if (ContentRegistry.boss(id) == null || type == null) {
                continue;
            }
            DataBossEntity e = type.create(c.world);
            if (e != null) {
                e.moveTo(at.x, at.y, at.z, 0, 0);
                e.setHealth(0); // its strikes land with no attacker
                return e;
            }
        }
        throw failure("no data boss type to host the probe");
    }

    /** One Minecraft tick of a probe script on the self-test clock (its tick, and its game tick on each boundary). */
    private static void probeMc(com.runecraft.boss.BossScript probe, DataBossEntity host) {
        ActionManager.advanceForSelfTest();
        probe.tick(host);
        if (ActionManager.mcTick() % ActionManager.MC_TICKS_PER_GAME_TICK == 0) {
            probe.gameTick(host, ActionManager.gameTick());
        }
    }

    /** Moves the self-test clock on to the next game-tick boundary and runs the probe's game tick there. */
    private static void probeAlign(com.runecraft.boss.BossScript probe, DataBossEntity host) {
        do {
            ActionManager.advanceForSelfTest();
            probe.tick(host);
        } while (ActionManager.mcTick() % ActionManager.MC_TICKS_PER_GAME_TICK != 0);
        probe.gameTick(host, ActionManager.gameTick());
    }

    /** The casts sent to the player while {@code body} runs. */
    private List<HudNet.SentCast> castsDuring(Runnable body) {
        List<HudNet.SentCast> sent = new ArrayList<>();
        java.util.function.Consumer<HudNet.SentCast> before = HudNet.castListener;
        HudNet.castListener = s -> {
            before.accept(s);
            if (s.player() == p()) {
                sent.add(s);
            }
        };
        try {
            body.run();
        } finally {
            HudNet.castListener = before;
        }
        return sent;
    }

    private static HudProbe.Cast castNamed(HudProbe.Snapshot s, String name) {
        for (HudProbe.Cast cast : s.casts()) {
            if (cast.name().equals(name)) {
                return cast;
            }
        }
        return null;
    }

    /**
     * Round 43: every telegraphed ground strike and hazard reaches the cast bar with its real landing. A scripted boss's strike
     * 3 blocks off (radius 1) puts a MOVE row on the bar counting down exactly the Minecraft ticks it then takes to land (the
     * 3-tick floor, unchanged), a volley of ten landing together is one row, a strike 10 blocks away none; a raid strike, a God
     * Wars strike and a hazard arming do the same, and a raid projectile shows in its style with the key that blocks it. On the
     * real client the rows show as MOVE (the projectile with the Protect from Magic key).
     */
    private String groundCasts() {
        ServerPlayer p = p();
        HudNet.selfTestPlayer = p; // the fake player isn't in the level's player list
        Vec3 at = p.position();
        DataBossEntity host = probeHost(at.add(0, 40, 0));
        StringBuilder note = new StringBuilder();
        try {
            // a scripted boss's strike near the player: one row, its landing the strike's real one
            ArenaCueChecks.Probe probe = new ArenaCueChecks.Probe();
            List<com.runecraft.osrs.ArenaTiming.StrikeTrace> traces = new ArrayList<>();
            java.util.function.Consumer<com.runecraft.osrs.ArenaTiming.StrikeTrace> was = ScriptedBoss.strikeListener;
            ScriptedBoss.strikeListener = was.andThen(traces::add);
            int[] took = new int[1];
            List<HudNet.SentCast> strike;
            try {
                probeAlign(probe, host);
                strike = castsDuring(() -> probe.fire(host, at.add(3, 0, 0), 1, 0, 1.0, (hit, pos) -> {
                }));
                if (client()) {
                    HudProbe.Snapshot s = awaitClient(x -> castNamed(x, "Arena probe") != null);
                    expect(s != null, "the client's cast bar never showed the scripted strike");
                    HudProbe.Cast row = castNamed(s, "Arena probe");
                    expect(row.key().equals("MOVE") && row.protection() < 0 && row.rightOverhead() == null,
                            "the strike's row shows key '" + row.key() + "' protection " + row.protection());
                    note.append("client: 'Arena probe' MOVE ").append(row.secondsLeft()).append("s; ");
                }
                while (probe.pending() && took[0] < 200) {
                    probeMc(probe, host);
                    took[0]++;
                }
            } finally {
                ScriptedBoss.strikeListener = was;
            }
            expect(strike.size() == 1, "a scripted strike 3 blocks off sent " + strike.size() + " cast-bar rows");
            HudNet.SentCast row = strike.get(0);
            expect(row.style() == CastBar.TYPELESS && row.flags() == CastBar.FLAG_DODGE && row.label().equals("Arena probe")
                    && row.source() == host.getId(), "the strike's row is " + row);
            expect(traces.size() == 1, traces.size() + " probe strikes landed");
            expect(row.landInMc() == took[0] && row.landInMc() >= com.runecraft.osrs.ArenaTiming.MIN_LEAD_MC,
                    "the row counted down " + row.landInMc() + " MC, the strike landed after " + took[0]);
            note.append("strike: row ").append(row.landInMc()).append(" MC, landed after ").append(took[0]);
            // a volley landing together is one row; a strike far off is none
            probeAlign(probe, host);
            List<HudNet.SentCast> volley = castsDuring(() -> {
                for (int i = 0; i < 10; i++) {
                    probe.fire(host, at.add(3, 0, i * 0.2 - 1), 2, 0, 1.0, (hit, pos) -> {
                    });
                }
                probe.fire(host, at.add(10, 0, 0), 2, 0, 1.0, (hit, pos) -> {
                });
            });
            expect(volley.size() == 1, "a volley of ten strikes (and one 10 blocks away) sent " + volley.size() + " rows");
            for (int i = 0; i < 200 && probe.pending(); i++) {
                probeMc(probe, host);
            }
            note.append("; volley of 10: 1 row");
            // the God Wars bosses' own strike list
            ArenaCueChecks.GwdProbe gwd = new ArenaCueChecks.GwdProbe();
            long[] due = new long[1];
            List<HudNet.SentCast> gwdRows = castsDuring(() -> {
                probeAlignGwd(gwd, host);
                due[0] = gwd.fire(host, at.add(3, 0, 0), 2, 10, 1.0);
            });
            expect(gwdRows.size() == 1 && gwdRows.get(0).flags() == CastBar.FLAG_DODGE
                    && gwdRows.get(0).landInMc() == (due[0] - ActionManager.gameTick()) * ActionManager.MC_TICKS_PER_GAME_TICK,
                    "a God Wars strike due in " + (due[0] - ActionManager.gameTick()) + " game ticks sent " + gwdRows);
            for (int i = 0; i < 200 && gwd.pending(); i++) {
                probeMc(gwd, host);
            }
            note.append("; God Wars strike ").append(gwdRows.get(0).landInMc()).append(" MC");
            // a raid strike and a raid projectile, both made to fizzle, landed here on the raid clock (nothing left pending for the
            // raid checks)
            double tag = 0.913;
            List<com.runecraft.osrs.ArenaTiming.StrikeTrace> raidLanded = new ArrayList<>();
            List<com.runecraft.content.raids.RaidEffects.ProjectileTrace> flights = new ArrayList<>();
            java.util.function.Consumer<com.runecraft.osrs.ArenaTiming.StrikeTrace> strikesBefore = com.runecraft.content.raids.RaidEffects.strikeListener;
            java.util.function.Consumer<com.runecraft.content.raids.RaidEffects.ProjectileTrace> flightsBefore =
                    com.runecraft.content.raids.RaidEffects.projectileListener;
            List<HudNet.SentCast> raid;
            List<HudNet.SentCast> shot;
            int ran = 0;
            try {
                com.runecraft.content.raids.RaidEffects.strikeListener = t -> {
                    strikesBefore.accept(t);
                    if (t.radius() == tag) {
                        raidLanded.add(t);
                    }
                };
                com.runecraft.content.raids.RaidEffects.projectileListener = t -> {
                    flightsBefore.accept(t);
                    flights.add(t);
                };
                raid = castsDuring(() -> com.runecraft.content.raids.RaidEffects.mark(c.world, at.add(3, 0, 0), tag)
                        .delay(1).damage(0, 5).live(() -> false).start());
                shot = castsDuring(() -> com.runecraft.content.raids.RaidEffects.projectile(c.world,
                        at.add(6, 2, 0), p, AttackType.MAGIC, 2, () -> false, () -> {
                        }));
                while ((raidLanded.isEmpty() || flights.isEmpty()) && ran < 400) {
                    ActionManager.advanceForSelfTest();
                    ran++;
                    if (ActionManager.mcTick() % ActionManager.MC_TICKS_PER_GAME_TICK == 0) {
                        com.runecraft.content.raids.RaidEffects.gameTick();
                    }
                }
                for (int i = 0; i < ActionManager.MC_TICKS_PER_GAME_TICK; i++) { // a game tick more: the spacing entry expires
                    ActionManager.advanceForSelfTest();
                    if (ActionManager.mcTick() % ActionManager.MC_TICKS_PER_GAME_TICK == 0) {
                        com.runecraft.content.raids.RaidEffects.gameTick();
                    }
                }
            } finally {
                com.runecraft.content.raids.RaidEffects.strikeListener = strikesBefore;
                com.runecraft.content.raids.RaidEffects.projectileListener = flightsBefore;
                com.runecraft.content.raids.RaidEffects.clear(c.world, new AABB(at, at).inflate(6));
            }
            expect(raid.size() == 1 && raid.get(0).flags() == CastBar.FLAG_DODGE && raid.get(0).label().equals("Strike")
                    && raid.get(0).landInMc() >= com.runecraft.osrs.ArenaTiming.MIN_LEAD_MC, "a raid strike sent " + raid);
            expect(raidLanded.size() == 1 && raidLanded.get(0).landedMc() - raidLanded.get(0).createdMc() == raid.get(0).landInMc(),
                    "the raid strike's row counted " + raid.get(0).landInMc() + " MC, it landed after "
                            + (raidLanded.isEmpty() ? "never" : raidLanded.get(0).landedMc() - raidLanded.get(0).createdMc()));
            expect(shot.size() == 1 && shot.get(0).style() == CastBar.MAGIC && shot.get(0).flags() == 0 && shot.get(0).landInMc() > 0,
                    "a raid magic projectile sent " + shot);
            expect(flights.size() == 1 && flights.get(0).flightMc() == shot.get(0).landInMc(), "the projectile's row counted "
                    + shot.get(0).landInMc() + " MC, it flew " + (flights.isEmpty() ? "never" : flights.get(0).flightMc()));
            note.append("; raid strike ").append(raid.get(0).landInMc()).append(" MC, projectile MAG ").append(shot.get(0).landInMc());
            // a hazard arming 2 blocks off
            List<HudNet.SentCast> pool = castsDuring(() -> Mechanics.addHazard(c.world, at.add(2, 0, 0), 1, 2, null));
            Mechanics.clearHazards(c.world, new AABB(at, at).inflate(4));
            expect(pool.size() == 1 && pool.get(0).flags() == CastBar.FLAG_DODGE && pool.get(0).label().equals("Hazard")
                    && pool.get(0).landInMc() == com.runecraft.osrs.ThreatTiming.HAZARD_ARM_MC, "a hazard sent " + pool);
            note.append("; hazard arms in ").append(pool.get(0).landInMc()).append(" MC");
            if (client()) {
                HudProbe.Snapshot s = awaitClient(x -> castNamed(x, "Strike") != null && castNamed(x, "Incoming") != null);
                expect(s != null, "the client's cast bar never showed the raid strike and projectile");
                expect(castNamed(s, "Strike").key().equals("MOVE"), "the raid strike's row shows " + castNamed(s, "Strike").key());
                HudProbe.Cast in = castNamed(s, "Incoming");
                expect(in.style() == CastBar.MAGIC && in.key().equals(s.protectionKeys()[0]),
                        "the projectile's row shows style " + in.style() + " key '" + in.key() + "'");
                note.append("; client: raid strike MOVE, projectile [").append(in.key()).append("]");
            }
            return note.toString();
        } finally {
            HudNet.selfTestPlayer = null;
            Mechanics.clearHazards(c.world, new AABB(at, at).inflate(4));
        }
    }

    private static void probeAlignGwd(ArenaCueChecks.GwdProbe probe, DataBossEntity host) {
        probeAlign(probe, host);
    }

    // ------------------------------------------------------------------ round 43: built-in boss bars and switch countdowns

    /** Built-in bosses (BossEntity) show the data bosses' status: Graardor's bar red, Karil's green, Ahrim's blue, Jad's white. */
    private String builtinBossBar() {
        StringBuilder note = new StringBuilder();
        Object[][] cases = {{com.runecraft.osrs.data.Boss.GENERAL_GRAARDOR, "RED"}, {com.runecraft.osrs.data.Boss.KARIL, "GREEN"},
                {com.runecraft.osrs.data.Boss.AHRIM, "BLUE"}, {com.runecraft.osrs.data.Boss.TZTOK_JAD, "WHITE"}};
        for (Object[] row : cases) {
            com.runecraft.osrs.data.Boss b = (com.runecraft.osrs.data.Boss) row[0];
            EntityType<com.runecraft.entity.BossEntity> type = ModEntities.BOSS_TYPES.get(b);
            expect(type != null, "no entity type for " + b);
            com.runecraft.entity.BossEntity boss = type.create(c.world);
            expect(boss != null, "couldn't create " + b);
            BlockPos pos = c.near(4, 0);
            boss.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
            boss.setNoAi(true);
            c.world.addFreshEntity(boss);
            try {
                boss.setTarget(p());
                boss.refreshBossBar();
                expect(boss.bossBarColor() == BossEvent.BossBarColor.valueOf((String) row[1]),
                        b.displayName() + "'s bar is " + boss.bossBarColor() + ", expected " + row[1]);
                expect(boss.bossBarTitle().equals(b.displayName()), b.displayName() + "'s bar reads '" + boss.bossBarTitle() + "'");
                note.append(b.displayName()).append(' ').append(boss.bossBarColor()).append("; ");
            } finally {
                boss.discard();
            }
        }
        return note.toString();
    }

    /**
     * The bosses whose switch the player must predict count it down on the boss bar: the Hunllef ("switches in 4", the style it
     * protects from), the Alchemical Hydra (phase, "switches in 3", the special) and Zulrah once surfaced (the next form).
     */
    private String switchCountdown() {
        StringBuilder note = new StringBuilder();
        String[][] cases = {{"crystalline_hunllef", "switches in 4", "protects "}, {"alchemical_hydra", "switches in 3", "special in "}};
        for (String[] row : cases) {
            BossDef def = ContentRegistry.boss(row[0]);
            if (def == null) {
                throw new SelfTest.Skip("no boss " + row[0]);
            }
            DataBossEntity boss = DataBossService.spawn(c.world, c.near(4, 0), def, p());
            expect(boss != null, "couldn't spawn " + row[0]);
            try {
                boss.refreshBossBar();
                String title = boss.bossBarTitle();
                expect(title.contains(row[1]) && title.contains(row[2]), row[0] + "'s bar reads '" + title + "', expected '" + row[1]
                        + "' and '" + row[2] + "'");
                expect(boss.bossBarColor() == BossEvent.BossBarColor.GREEN, row[0] + " opens with ranged: its bar is " + boss.bossBarColor());
                note.append('\'').append(title).append("'; ");
            } finally {
                boss.discard();
            }
        }
        BossDef zulrah = ContentRegistry.boss("zulrah");
        if (zulrah != null) {
            DataBossEntity boss = DataBossService.spawn(c.world, c.near(4, 0), zulrah, p());
            expect(boss != null, "couldn't spawn zulrah");
            try {
                boss.script().gameTick(boss, ActionManager.gameTick()); // surfaces in its rotation's first phase
                boss.refreshBossBar();
                String title = boss.bossBarTitle();
                expect(title.contains("next: crimson (melee)") || title.contains("next: green (ranged)") || title.contains("next: blue (magic)"),
                        "Zulrah's bar reads '" + title + "', expected its next form");
                note.append('\'').append(title).append('\'');
            } finally {
                boss.discard();
            }
        }
        return note.toString();
    }

    // ------------------------------------------------------------------ round 43: Sol Heredit's grapple prompt

    /**
     * Sol Heredit's grapple prompt reaches the client: the called slot, the window, the bound grapple keys in its text; the
     * answer from a grapple key goes through his parry rule; clearing it takes it off the screen.
     */
    private String grapplePrompt() {
        ServerPlayer p = p();
        List<HudNet.SentGrapple> sent = new ArrayList<>();
        java.util.function.Consumer<HudNet.SentGrapple> before = HudNet.grappleListener;
        HudNet.grappleListener = g -> {
            before.accept(g);
            if (g.player() == p) {
                sent.add(g);
            }
        };
        try {
            int legs = com.runecraft.osrs.content.minigames.SolHeredit.Grapple.LEGS.ordinal();
            int window = com.runecraft.osrs.content.minigames.SolHeredit.GRAPPLE_WINDOW_TICKS * ActionManager.MC_TICKS_PER_GAME_TICK;
            expect(window == 72, "the grapple window is " + window + " MC, not 3.6 s");
            HudNet.grapple(p, legs, window);
            expect(sent.size() == 1 && HudNet.grappleOpen(p), "the prompt sent " + sent);
            String note = "legs, " + window + " MC";
            if (client()) {
                HudProbe.Snapshot s = awaitClient(x -> x.grappleCalled() == legs);
                expect(s != null, "the client never showed the grapple prompt");
                expect(s.grapplePrompt().startsWith("GRAPPLE: press") && s.grapplePrompt().contains(" legs")
                        && s.grapplePrompt().contains(" body"), "the prompt reads '" + s.grapplePrompt() + "'");
                note += "; client: '" + s.grapplePrompt() + "'";
            }
            // no grapple of Sol's on this player: the answer is refused by his parry rule, and the prompt goes
            expect(!HudNet.answerGrapple(p, legs), "a grapple key blocked a grapple nobody called");
            HudNet.update(p);
            expect(!HudNet.grappleOpen(p) && sent.get(sent.size() - 1).called() == -1, "the prompt stayed: " + sent);
            if (client()) {
                expect(awaitClient(x -> x.grappleCalled() < 0) != null, "the client kept the grapple prompt");
            }
            return note;
        } finally {
            HudNet.grappleListener = before;
            HudNet.grapple(p, -1, 0);
        }
    }

    // ------------------------------------------------------------------ round 43: the food delay on the attack timer

    /**
     * Food eaten during a running weapon delay adds to it (3 ticks, OSRS) and tells the client, whose attack timer then shows
     * that wait as a food delay instead of restarting; eaten with the weapon ready it adds nothing.
     */
    private String foodDelay() {
        ServerPlayer p = p();
        List<Integer> told = new ArrayList<>();
        java.util.function.BiConsumer<ServerPlayer, Integer> before = HudNet.foodDelayListener;
        HudNet.foodDelayListener = (pl, mc) -> {
            before.accept(pl, mc);
            if (pl == p) {
                told.add(mc);
            }
        };
        try {
            data().lastAttackTick = ActionManager.mcTick() - 10_000;
            expect(com.runecraft.combat.AttackTimer.addDelay(p, 36) == 0 && told.isEmpty(), "food with the weapon ready added a delay");
            com.runecraft.combat.AttackTimer.accept(p, com.runecraft.osrs.AttackClock.Kind.MELEE, 48);
            int added = com.runecraft.combat.AttackTimer.addDelay(p, 36);
            expect(added == 36 && told.equals(List.of(36)), "food during the weapon delay added " + added + ", told " + told);
            long left = com.runecraft.combat.AttackTimer.remaining(p);
            expect(left == 48 + 36, "the next attack is " + left + " MC away, expected 84 (48 + 36)");
            String note = "added 36 MC to a 48 MC weapon delay";
            if (client()) {
                HudProbe.Snapshot s = awaitClient(x -> x.foodDelayMc() > 0);
                expect(s != null, "the client's attack timer never showed the food delay");
                expect(s.foodDelayMc() <= 36, "the client shows " + s.foodDelayMc() + " MC of food delay, more than the 36 added");
                note += "; client timer shows " + s.foodDelayMc() + " MC of food delay";
            }
            return note;
        } finally {
            HudNet.foodDelayListener = before;
            data().lastAttackTick = ActionManager.mcTick() - 10_000;
        }
    }

    private int arrowsNear(Vec3 at, double r) {
        int n = 0;
        for (ItemEntity e : c.world.getEntitiesOfClass(ItemEntity.class, new AABB(at, at).inflate(r, 1.0, r))) {
            if ("rune_arrow".equals(Inv.idOf(e.getItem()))) {
                n += e.getItem().getCount();
            }
        }
        return n;
    }

    private void clearArrows(Vec3 at) {
        for (ItemEntity e : c.world.getEntitiesOfClass(ItemEntity.class, new AABB(at, at).inflate(4, 2, 4))) {
            e.discard();
        }
    }
}
